package ca.mcgill.mcb.pcingola.bigDataScript.task;

import java.io.File;
import java.util.List;

import ca.mcgill.mcb.pcingola.bigDataScript.cluster.host.HostResources;
import ca.mcgill.mcb.pcingola.bigDataScript.serialize.BigDataScriptSerialize;
import ca.mcgill.mcb.pcingola.bigDataScript.serialize.BigDataScriptSerializer;
import ca.mcgill.mcb.pcingola.bigDataScript.util.Gpr;
import ca.mcgill.mcb.pcingola.bigDataScript.util.Timer;

/**
 * A task to be executed by an Executioner
 * 
 * @author pcingola
 */
public class Task implements BigDataScriptSerialize {

	public enum TaskState {
		NONE // Task created, nothing happened so far
		, STARTED // Process started (or queued for execution)
		, START_FAILED // Process failed to start (or failed to queue)
		, RUNNING // Running OK
		, ERROR // Filed while running
		, ERROR_TIMEOUT // Filed due to timeout
		, KILLED // Task was killed  
		, FINISHED // Finished OK  
		;

		public static TaskState exitCode2taskState(int exitCode) {
			switch (exitCode) {
			case EXITCODE_OK:
				return FINISHED;

			case EXITCODE_ERROR:
				return ERROR;

			case EXITCODE_TIMEOUT:
				return ERROR_TIMEOUT;

			case EXITCODE_KILLED:
				return KILLED;

			default:
				return ERROR;
			}

		}
	}

	// TODO: This should be a variable (SHEBANG?)
	public static final String SHE_BANG = "#!/bin/sh -e\n\n"; // Use '-e' so that shell script stops after first error

	// Exit codes (see bds.go)
	public static final int EXITCODE_OK = 0;
	public static final int EXITCODE_ERROR = 1;
	public static final int EXITCODE_TIMEOUT = 2;
	public static final int EXITCODE_KILLED = 3;

	protected boolean verbose, debug;
	protected boolean canFail; // Allow execution to fail
	protected int bdsLineNum; // Program's line number that created this task (used for reporting errors)
	protected int exitValue; // Exit (error) code
	protected String id; // Task ID
	protected String bdsFileName; // Program file that created this task (used for reporting errors)
	protected String pid; // PID (if any)
	protected String programFileName; // Program file name
	protected String programTxt; // Program's text (program's code)
	protected String node; // Preferred execution node (or hostname)
	protected String queue; // Preferred execution queue
	protected String stdoutFile, stderrFile, exitCodeFile; // STDOUT, STDERR & exit code Files
	protected TaskState taskState;
	protected HostResources resources; // Resources to be consumes when executing this task
	protected List<String> outputFiles; // Output files generated by this task. TODO Serialize this!
	protected String checkOutputFiles;

	public Task() {
		resources = new HostResources();
		reset();
	}

	public Task(String id, String programFileName, String programTxt, String bdsFileName, int bdsLineNum) {
		this.id = id;
		this.programFileName = programFileName;
		this.programTxt = programTxt;
		this.bdsFileName = bdsFileName;
		this.bdsLineNum = bdsLineNum;
		resources = new HostResources();
		reset();
	}

	/**
	 * Can this task run?
	 * I.e.: It has not been started yet and all dependencies are satisfied
	 * @return true if we are ready to run this task
	 */
	public boolean canRun() {
		return taskState == TaskState.NONE;
	}

	/**
	 * Check if output files are OK
	 * @return true if OK, false there is an error (output file does not exist or has zero length)
	 */
	protected String checkOutputFiles() {
		if (checkOutputFiles != null) return checkOutputFiles;
		if (!isStateFinished() || outputFiles == null) return ""; // Nothing to check

		checkOutputFiles = "";
		for (String fileName : outputFiles) {
			File file = new File(fileName);
			if (!file.exists()) checkOutputFiles += "Error: Output file '" + fileName + "' does not exist";
			else if (file.length() <= 0) checkOutputFiles += "Error: Output file '" + fileName + "' has zero length";
		}

		if (verbose && !checkOutputFiles.isEmpty()) Timer.showStdErr(checkOutputFiles);
		return checkOutputFiles;
	}

	/**
	 * Create a program file
	 */
	public void createProgramFile() {
		Gpr.toFile(programFileName, SHE_BANG + programTxt);
		(new File(programFileName)).setExecutable(true); // Allow execution onf this file

		// Set default file names
		String base = Gpr.removeExt(programFileName);
		if (stdoutFile == null) stdoutFile = base + ".stdout";
		if (stderrFile == null) stderrFile = base + ".stderr";
		if (exitCodeFile == null) exitCodeFile = base + ".exitCode";
	}

	/**
	 * Remove tmp files on exit
	 */
	public void deleteOnExit() {
		if (stdoutFile != null) (new File(stdoutFile)).deleteOnExit();
		if (stderrFile != null) (new File(stderrFile)).deleteOnExit();
		if (exitCodeFile != null) (new File(exitCodeFile)).deleteOnExit();
		if (programFileName != null) (new File(programFileName)).deleteOnExit();
	}

	/**
	 * Mark output files to be deleted on exit
	 */
	public void deleteOutputFilesOnExit() {
		if (outputFiles == null) return; // Nothing to check

		for (String fileName : outputFiles) {
			File file = new File(fileName);
			if (file.exists()) file.deleteOnExit();
		}
	}

	public String getBdsFileName() {
		return bdsFileName;
	}

	public int getBdsLineNum() {
		return bdsLineNum;
	}

	public String getExitCodeFile() {
		return exitCodeFile;
	}

	public synchronized int getExitValue() {
		if (!checkOutputFiles().isEmpty()) return 1; // Any output file failed?
		return exitValue;
	}

	public String getId() {
		return id;
	}

	public String getNode() {
		return node;
	}

	public synchronized String getPid() {
		return pid;
	}

	public String getProgramFileName() {
		return programFileName;
	}

	public String getQueue() {
		return queue;
	}

	public HostResources getResources() {
		return resources;
	}

	public String getStderrFile() {
		return stderrFile;
	}

	public String getStdoutFile() {
		return stdoutFile;
	}

	public TaskState getTaskState() {
		return taskState;
	}

	public boolean isCanFail() {
		return canFail;
	}

	/**
	 * Has this task finished? Either finished OK or finished because of errors.
	 * @return
	 */
	public synchronized boolean isDone() {
		return isError() || isStateFinished();
	}

	/**
	 * Has this task been executed successfully?
	 * The task has finished, exit code is zero and all output files have been created
	 * 
	 * @return
	 */
	public synchronized boolean isDoneOk() {
		return isStateFinished() && (exitValue == 0) && checkOutputFiles().isEmpty();
	}

	/**
	 * Is this task in any error or killed state?
	 * @return
	 */
	public synchronized boolean isError() {
		return (taskState == TaskState.START_FAILED) //
				|| (taskState == TaskState.ERROR) //
				|| (taskState == TaskState.ERROR_TIMEOUT) //
				|| (taskState == TaskState.KILLED) //
		;
	}

	/**
	 * Has this task been executed and failed?
	 * 
	 * This is true if:
	 * 		- The task has finished execution and it is in an error state 
	 * 		- OR exitValue is non-zero 
	 * 		- OR any of the output files was not created
	 * 
	 * @return
	 */
	public synchronized boolean isFailed() {
		return isError() || (exitValue != 0) || !checkOutputFiles().isEmpty();
	}

	/**
	 * Has the task been started?
	 * @return
	 */
	public synchronized boolean isStarted() {
		return taskState != TaskState.NONE;
	}

	public synchronized boolean isStateFinished() {
		return taskState == TaskState.FINISHED;
	}

	public synchronized boolean isStateRunning() {
		return taskState == TaskState.RUNNING;
	}

	public synchronized boolean isStateStarted() {
		return taskState == TaskState.STARTED;
	}

	/**
	 * Reset parameters and allow a task to be re-executed
	 */
	public void reset() {
		taskState = TaskState.NONE;
		exitValue = 0;
		outputFiles = null;
	}

	@Override
	public void serializeParse(BigDataScriptSerializer serializer) {
		// Note that "Task classname" field has been consumed at this point
		id = serializer.getNextField();
		canFail = serializer.getNextFieldBool();
		taskState = TaskState.valueOf(serializer.getNextFieldString());
		exitValue = (int) serializer.getNextFieldInt();
		node = serializer.getNextField();
		queue = serializer.getNextField();
		programFileName = serializer.getNextFieldString();
		programTxt = serializer.getNextFieldString();
		stdoutFile = serializer.getNextFieldString();
		stderrFile = serializer.getNextFieldString();
		exitCodeFile = serializer.getNextFieldString();

		resources = new HostResources();
		resources.serializeParse(serializer);
	}

	@Override
	public String serializeSave(BigDataScriptSerializer serializer) {
		return getClass().getSimpleName() //
				+ "\t" + id // 
				+ "\t" + canFail // 
				+ "\t" + taskState // 
				+ "\t" + exitValue // 
				+ "\t" + node // 
				+ "\t" + queue // 
				+ "\t" + serializer.serializeSaveValue(programFileName) //
				+ "\t" + serializer.serializeSaveValue(programTxt) //
				+ "\t" + serializer.serializeSaveValue(stdoutFile) //
				+ "\t" + serializer.serializeSaveValue(stderrFile) //
				+ "\t" + serializer.serializeSaveValue(exitCodeFile) //
				+ "\t" + resources.serializeSave(serializer) //
				+ "\n";
	}

	public void setCanFail(boolean canFail) {
		this.canFail = canFail;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Set program's exit value and update state accordingly
	 * @param exitValue
	 */
	public synchronized void setExitValue(int exitValue) {
		this.exitValue = exitValue;
		if (exitValue == EXITCODE_OK) setState(TaskState.FINISHED);
		else if (exitValue == EXITCODE_TIMEOUT) setState(TaskState.ERROR_TIMEOUT);
		else if (exitValue == EXITCODE_KILLED) setState(TaskState.KILLED);
		else setState(TaskState.ERROR);
	}

	public void setNode(String node) {
		this.node = node;
	}

	public void setOutputFiles(List<String> outputFiles) {
		this.outputFiles = outputFiles;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	private void setState(TaskState taskState) {
		this.taskState = taskState;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Change state: Make sure state changes are valid
	 * @param taskState
	 */
	public synchronized void state(TaskState taskState) {
		if (taskState == null) throw new RuntimeException("Cannot change to 'null' state.\n" + this);
		if (taskState == this.taskState) return; // Nothing to do

		switch (taskState) {
		case STARTED:
		case START_FAILED:
			if (this.taskState == TaskState.NONE) setState(taskState);
			else throw new RuntimeException("Task: Cannot jump from state '" + this.taskState + "' to state '" + taskState + "'\n" + this);
			break;

		case RUNNING:
			if (this.taskState == TaskState.STARTED) setState(taskState);
			else throw new RuntimeException("Task: Cannot jump from state '" + this.taskState + "' to state '" + taskState + "'\n" + this);
			break;

		case FINISHED:
		case ERROR:
		case ERROR_TIMEOUT:
			if (this.taskState == TaskState.RUNNING) setState(taskState);
			else throw new RuntimeException("Task: Cannot jump from state '" + this.taskState + "' to state '" + taskState + "'\n" + this);
			break;

		case KILLED:
			if ((this.taskState == TaskState.RUNNING) // A task can be killed while running...
					|| (this.taskState == TaskState.STARTED) // or right after it started
					|| (this.taskState == TaskState.NONE) // or even if it was not started
			) setState(taskState);
			else throw new RuntimeException("Task: Cannot jump from state '" + this.taskState + "' to state '" + taskState + "'\n" + this);
			break;

		default:
			throw new RuntimeException("Unimplemented state: '" + taskState + "'");
		}
	}

	@Override
	public String toString() {
		return toString(verbose);
	}

	public String toString(boolean verbose) {
		StringBuilder sb = new StringBuilder();

		if (verbose) {
			sb.append("\tProgram & line     : '" + bdsFileName + "', line " + bdsLineNum + "\n");
			sb.append("\tTask ID            : '" + id + "'\n");
			sb.append("\tTask state         : '" + taskState + "'\n");
			sb.append("\tScript file        : '" + programFileName + "'\n");
			sb.append("\tExit status        : '" + exitValue + "'\n");

			String ch = checkOutputFiles();
			if ((ch != null) && !ch.isEmpty()) sb.append("\tOutput file errors :\n" + Gpr.prependEachLine("\t\t", ch));

			String tailErr = TailFile.tail(stderrFile);
			if ((tailErr != null) && !tailErr.isEmpty()) sb.append("\tStdErr (10 lines)  :\n" + Gpr.prependEachLine("\t\t", tailErr));

			String tailOut = TailFile.tail(stdoutFile);
			if ((tailOut != null) && !tailOut.isEmpty()) sb.append("\tStdOut (10 lines)  :\n" + Gpr.prependEachLine("\t\t", tailOut));

			if (debug) {
				sb.append("\tTask raw code:\n");
				sb.append("-------------------- Task code: Start --------------------\n");
				sb.append(programTxt + "\n");
				sb.append("-------------------- Task code: End   --------------------\n");
			}
		} else sb.append("'" + bdsFileName + "', line " + bdsLineNum);

		return sb.toString();
	}

}

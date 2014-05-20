I've added some command line parameters that allow you to run a SSM program from the command line. You can specify a file or use stdin as input. The program output is printed to stdout and no GUI is loaded. Using the --clisteps parameter you can specify maxiumum amount of instructions that should be done to prevent infinite execution time.

Command line arguments:
```
usage: [--clisteps <steps>] [--cli] [--file <path> OR --stdin]
	--clisteps <steps>: The amount of steps to run. -1 for infinite(default). Only in cli mode
	--stdin: Read code from stdin
	--file <path>: Read code from path
	--cli: No GUI, runs code and exits on halt
	--guidelay: Amount of time to sleep in milliseconds between steps in the GUI. Default: 50
```

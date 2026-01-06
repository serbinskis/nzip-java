# nzip

This is a Java-based file compression utility that supports both GUI and command-line interfaces. It provides functionalities for compressing and decompressing files using different compression algorithms. It was made as a university project, so don't expect anything good, because it was made in a week and improved little by little later.

<p align="center"><img src="https://i.imgur.com/P2iRrJN.png"/></p>

## Features:

- GUI Interface: Provides a user-friendly graphical interface for selecting files and compression options.
- Command-Line Interface: Offers a command-line interface for users who prefer working with a terminal.
- Multiple Compression Algorithms: Supports various compression algorithms such as DEFLATE, with the option to add more.
- Progress Tracking: Displays progress during compression and decompression processes.
- File Size Comparison: Allows users to compare the sizes of compressed and decompressed files.
- File Content Equality Check: Enables users to check if the content of two files is identical.

## Installation:

To use the GUI version of the utility, simply clone the repository and compile the Java files using a Java compiler. Then, run the Form class to launch the GUI.

For the command-line version, compile the Main class and run the generated executable JAR file with appropriate command-line arguments.

## GUI Interface:

1. Launch the application.
2. Click "Select File" to choose the file you want to compress or decompress.
3. Choose the desired compression algorithm from the dropdown list.
4. Click "Compress" or "Decompress" to start the process.
5. Monitor the progress in the progress bar and view the status in the status label.
6. Once the process is complete, a message will indicate success or failure.

## Command-Line Interface:
1. comp: Compress a file.
2. decomp: Decompress a file.
3. size: Get the size of a file.
4. equal: Check if two files are equal.

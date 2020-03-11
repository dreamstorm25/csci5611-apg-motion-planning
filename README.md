# Motion planning
- Code is written in java 11, but should work with 8+
- src/ contains all source code
- jars/ contain all libraries bundled as jars
    - processing is used as a library

# Compilation (on linux)
- Open a terminal with current directory as the one containing this file
- Use `javac -cp "jars/*" -d build/ src/*.java src/*/*.java src/*/*/*.java` to compile and put all output class files under build/
- Use `java -cp "build/:jars/*" demos.<simulation>` to run any simulation
- Use `java -cp "build/:jars/*" Main` to run main simulation

# Compilation (on windows)
- Install ant and use it for building
- Open a terminal in current directory and run `ant compile` to build the project.
- To execute the **main class** run this command next `java -cp "bin/;jars/*" Main`
- To execute any **demo class** run this command `java -cp "bin/;jars/*" demos.<simulation>`

# Things used
- Queasy cam

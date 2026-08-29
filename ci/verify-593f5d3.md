### Verify run @ 2026-08-29T20:01:19Z on commit 593f5d3bbee3867b7839ee723572d02cf6b88b8b

RESULT: FAIL
exit codes: compile=0 test=1 package=0

#### tests summary
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.299 s -- in com.bx.ultimateDonutSmp.storage.ShopPreferenceRepositoryTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.080 s -- in com.bx.ultimateDonutSmp.storage.AuctionHouseRepositoryTest
[ERROR] Tests run: 417, Failures: 1, Errors: 0, Skipped: 0

#### test log errors
[ERROR] Tests run: 11, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.013 s <<< FAILURE! -- in com.bx.ultimateDonutSmp.managers.PingManagerTest
[ERROR] com.bx.ultimateDonutSmp.managers.PingManagerTest.managerAvoidsModernApiAndTickingTasks -- Time elapsed: 0.004 s <<< FAILURE!
[ERROR] Failures: 
[ERROR]   PingManagerTest.managerAvoidsModernApiAndTickingTasks:118 refresh cadence must stay at 5 s (100 ticks) ==> expected: <true> but was: <false>
[ERROR] Tests run: 417, Failures: 1, Errors: 0, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project ultimatedonutsmp: There are test failures.
[ERROR] 
[ERROR] Please refer to /home/runner/work/UltimateDonutSMP-main/UltimateDonutSMP-main/target/surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
#### compile log tail
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------------< com.bx:ultimatedonutsmp >-----------------------
[INFO] Building ultimatedonutsmp 1.5
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ultimatedonutsmp ---
[INFO] Copying 36 resources from src/main/resources to target/classes
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ultimatedonutsmp ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 425 source files with javac [debug target 1.8] to target/classes
[INFO] /home/runner/work/UltimateDonutSMP-main/UltimateDonutSMP-main/src/main/java/com/bx/ultimateDonutSmp/managers/DatabaseManager.java: Some input files use or override a deprecated API.
[INFO] /home/runner/work/UltimateDonutSMP-main/UltimateDonutSMP-main/src/main/java/com/bx/ultimateDonutSmp/managers/DatabaseManager.java: Recompile with -Xlint:deprecation for details.
[INFO] /home/runner/work/UltimateDonutSMP-main/UltimateDonutSMP-main/src/main/java/com/bx/ultimateDonutSmp/managers/PlayerWipeManager.java: Some input files use unchecked or unsafe operations.
[INFO] /home/runner/work/UltimateDonutSMP-main/UltimateDonutSMP-main/src/main/java/com/bx/ultimateDonutSmp/managers/PlayerWipeManager.java: Recompile with -Xlint:unchecked for details.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  10.863 s
[INFO] Finished at: 2026-08-29T20:00:58Z
[INFO] ------------------------------------------------------------------------
#### package log tail
[INFO] BUILD SUCCESS
#### jar audit
JAR: target/UltimateDonutSmp-1.5.jar
 51014843                     5339 files
== modern-API reference scan in plugin classes ==
no obvious modern refs in com/bx

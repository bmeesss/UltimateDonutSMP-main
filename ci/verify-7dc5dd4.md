### Verify run @ 2026-08-29T19:18:20Z on commit 7dc5dd490ecc1849f2a1cc1ab1f0cd23470e534c

RESULT: PASS
exit codes: compile=0 test=0 package=0

#### tests summary
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.053 s -- in com.bx.ultimateDonutSmp.storage.ShopPreferenceRepositoryTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.104 s -- in com.bx.ultimateDonutSmp.storage.AuctionHouseRepositoryTest
[INFO] Tests run: 407, Failures: 0, Errors: 0, Skipped: 0

#### test log errors
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
[INFO] Total time:  7.783 s
[INFO] Finished at: 2026-08-29T19:17:55Z
[INFO] ------------------------------------------------------------------------
#### package log tail
[INFO] BUILD SUCCESS
#### jar audit
JAR: target/UltimateDonutSmp-1.5.jar
 51014438                     5339 files
== modern-API reference scan in plugin classes ==
no obvious modern refs in com/bx

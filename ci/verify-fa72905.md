### Verify run @ 2026-08-29T17:52:12Z on commit fa72905c1848e8db6b390cb7d4f6039837c93c55

RESULT: FAIL
exit codes: compile=0 test=1 package=0

#### tests summary
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.022 s -- in com.bx.ultimateDonutSmp.storage.ShopPreferenceRepositoryTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.107 s -- in com.bx.ultimateDonutSmp.storage.AuctionHouseRepositoryTest
[ERROR] Tests run: 395, Failures: 2, Errors: 0, Skipped: 0

#### test log errors
[ERROR] Tests run: 6, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 0.119 s <<< FAILURE! -- in com.bx.ultimateDonutSmp.managers.WorthLoreFormatTest
[ERROR] com.bx.ultimateDonutSmp.managers.WorthLoreFormatTest.placeholderReplacementRendersExactlyOneDollarSign -- Time elapsed: 0.004 s <<< FAILURE!
[ERROR] com.bx.ultimateDonutSmp.managers.WorthLoreFormatTest.everyLanguageMirrorStaysInSync -- Time elapsed: 0.043 s <<< FAILURE!
[ERROR] Failures: 
[ERROR]   WorthLoreFormatTest.everyLanguageMirrorStaysInSync:95 de_DE.yml mirrors the live DISPLAY.FORMAT; a drifted copy would resurrect the doubled sign the moment localization starts consuming the mirror ==> expected: <&7Worth: &a${price}> but was: <&7Wert: &a${price}>
[ERROR]   WorthLoreFormatTest.placeholderReplacementRendersExactlyOneDollarSign:68 {price_raw} already embeds the symbol via formatMoney; a lone $ in the format would double it for admins copying that style, so the placeholder alone must carry the currency ==> expected: <&7Worth: &$1,250.00> but was: <&7Worth: &a$1,250.00>
[ERROR] Tests run: 395, Failures: 2, Errors: 0, Skipped: 0
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
[INFO] Total time:  9.755 s
[INFO] Finished at: 2026-08-29T17:51:41Z
[INFO] ------------------------------------------------------------------------
#### package log tail
[INFO] BUILD SUCCESS
#### jar audit
JAR: target/UltimateDonutSmp-1.5.jar
 51010008                     5339 files
== modern-API reference scan in plugin classes ==
no obvious modern refs in com/bx

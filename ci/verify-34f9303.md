### Verify run @ 2026-08-29T17:01:35Z on commit 34f93035d5cdd6d4366e914c209087c38e9c4914

RESULT: FAIL

#### tests summary
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.028 s -- in com.bx.ultimateDonutSmp.storage.ShopPreferenceRepositoryTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.107 s -- in com.bx.ultimateDonutSmp.storage.AuctionHouseRepositoryTest
[ERROR] Tests run: 389, Failures: 1, Errors: 0, Skipped: 0

#### compile log tail
#### test log errors
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.037 s <<< FAILURE! -- in com.bx.ultimateDonutSmp.utils.TablistComponentLegacyTextTest
[ERROR] com.bx.ultimateDonutSmp.utils.TablistComponentLegacyTextTest.coloursAndStylesBecomeLegacyCodes -- Time elapsed: 0.005 s <<< FAILURE!
[ERROR] Failures: 
[ERROR]   TablistComponentLegacyTextTest.coloursAndStylesBecomeLegacyCodes:27 expected: <§c§lOwner§e Notch> but was: <§c§lOwner§6 Notch>
[ERROR] Tests run: 389, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
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
#### package log tail
[INFO] BUILD SUCCESS
#### jar audit
JAR: target/UltimateDonutSmp-1.5.jar
 51010186                     5339 files
== modern-API reference scan in plugin classes ==

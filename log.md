> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :app:dataBindingMergeDependencyArtifactsDebug UP-TO-DATE
> Task :app:generateDebugResValues UP-TO-DATE
> Task :app:generateDebugResources UP-TO-DATE
> Task :app:mergeDebugResources UP-TO-DATE
> Task :app:packageDebugResources UP-TO-DATE
> Task :app:parseDebugLocalResources UP-TO-DATE
> Task :app:dataBindingGenBaseClassesDebug UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:checkDebugAarMetadata UP-TO-DATE
> Task :app:mapDebugSourceSetPaths UP-TO-DATE
> Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
> Task :app:extractDeepLinksDebug UP-TO-DATE
> Task :app:processDebugMainManifest UP-TO-DATE
> Task :app:processDebugManifest UP-TO-DATE
> Task :app:processDebugManifestForPackage UP-TO-DATE
> Task :app:processDebugResources UP-TO-DATE

> Task :app:compileDebugJavaWithJavac FAILED
C:\Users\Dell\Desktop\Weather app\app\src\main\java\com\weatherapp\app\HomeActivity.java:323: error: cannot find symbol
        binding.pressureVal.setText(pressure + "%"); // Layout says Rain/Humidity, let's map pressure to Rain for now as placeholder or keep logic valid?
               ^
  symbol:   variable pressureVal
  location: variable binding of type ActivityHomeBinding
C:\Users\Dell\Desktop\Weather app\app\src\main\java\com\weatherapp\app\HomeActivity.java:328: error: cannot find symbol
        binding.windVal.setText(wind_speed + " km/h");
               ^
  symbol:   variable windVal
  location: variable binding of type ActivityHomeBinding
C:\Users\Dell\Desktop\Weather app\app\src\main\java\com\weatherapp\app\HomeActivity.java:329: error: cannot find symbol
        binding.humidityVal.setText(humidity); // mapped to SO2 in layout
               ^
  symbol:   variable humidityVal
  location: variable binding of type ActivityHomeBinding
C:\Users\Dell\Desktop\Weather app\app\src\main\java\com\weatherapp\app\HomeActivity.java:330: error: cannot find symbol
        binding.pressureVal.setText(pressure + " mb"); // mapped to Rain in layout
               ^
  symbol:   variable pressureVal
  location: variable binding of type ActivityHomeBinding
C:\Users\Dell\Desktop\Weather app\app\src\main\java\com\weatherapp\app\HomeActivity.java:331: error: cannot find symbol
        binding.realFeelVal.setText(temperature + "°"); // Placeholder
               ^
  symbol:   variable realFeelVal
  location: variable binding of type ActivityHomeBinding
Note: Some input files use or override a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
5 errors

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugJavaWithJavac'.
> Compilation failed; see the compiler error output for details.

* Try:
> Run with --info option to get more log output.
> Run with --scan to get full insights.

BUILD FAILED in 3s
16 actionable tasks: 1 executed, 15 up-to-date

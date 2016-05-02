Creates specific zip files to be deployed containing information on how to profile an algorithm.

To use in Maven single project build:

      <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-assembly-plugin</artifactId>
          <version>2.6</version>
          <dependencies>
              <dependency>
                  <groupId>eu.qualimaster</groupId>
                  <artifactId>AlgorithmAssemblies</artifactId>
                  <version>0.5.0-SNAPSHOT</version>
              </dependency>
          </dependencies>             
          <executions>
              <execution>
                <id>make-profilingAssembly</id>
                <phase>package</phase>
                <goals>
                  <goal>single</goal>
                </goals>
                <configuration>
                  <descriptorRefs>
                      <descriptorRef>profiling</descriptorRef>
                  </descriptorRefs>
                </configuration>
              </execution>
          </executions>
       </plugin>
       
  If the maven-assembly-plugin is already used, in particular enable version 2.6, add the dependencies section and 
  the make-profilingAssembly execution. If for some reasons the AlgorithmAssemblies artifact cannot be resolved, just
  list it also in the normal dependencies of your project. If you use the algorithm dependencies via dependency 
  management, you may even leave out the version number.
  
  Create two files (formats -> Wiki), namely profile.ctl and profile.data either in 
    /profiling
    /src/main/profiling
    /src/main/resources/profiling
  while /profiling is the most safe place that these files are not also (automatically) included in the binary builds.

To use in Maven multi module build:
  - Add to the maven-assembly-plugin with the dependency and the execution shown above to the main pom.
  - In the child projects, add the profile.ctl/.data files in one of the locations described above.
  - If a child shall not be considered for profiling, add the overriding section
  
    <build>
     <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <executions>
                    <execution>
                        <id>make-profilingAssembly</id>
                        <phase>none</phase>
                    </execution>
                </executions>
            </plugin>
     </plugins>
    </build> 
  
    to the child pom. Note that it is important to refer properly to the maven-assembly-plugin (version not needed
    as defined in parent) and the overriding execution with the correct id (make-profilingAssembly) and the phase "none".
    
    If for some reasons the AlgorithmAssemblies artifact cannot be resolved, just list it also in the normal 
    dependencies of your project. If you use the algorithm dependencies via dependency management, you may even leave 
    out the version number.

//
// ????
// Anek
//

Components:
  GraphExtractor - An eclipse plugin for generating xml representations of Java programs. Generates
                   XML files that are instances of the xml/graphml+plural.xsd schema. This plugin also
                   reloads XML files after inference and applies them to a selected project.

  GraphLoader - An F# program that generates constraints for permissions, solves them, and generates a
                new graph representation with the inferred permissions in place.


Assumptions:
  - GraphLoader assumes that all node IDs are unique, even from method to method.

Running Anek:
  Running Anek is separated into two parts. First you need the Anek Eclipse plugin, which is attached to
  Plural. The second thing you need is the F# program.

  Installing the Anek Eclipse Plugin:
  1.) The first thing you will need is Eclipse. The Anek plugin probably works with many versions of
      Eclipse, but it's best to try 3.5, since that's the version we tried it with. To download Eclipse,
      go to the following web site:
      http://www.eclipse.org/downloads/
      There are several packages. You should pick the package called, "Eclipse Classic," which is shown
      all the way at the bottom of the list.
  2.) Once Eclipse is up in running, you'll need three additional plugins, Crystal, Plural and Plaid
      annotations. These are all plugins necessary to make Plural work, upon which Anek depends.
      There is a standard procedure for installing Eclipse plugins from the web: Go to the "Help" menu,
      select "Install New Software," select "Add..." which will allow you to add a new web location
      from which to download the plugins. This process will be done three times, to install each of the
      three plugins. First, install Plaid annotations, giving Eclipse the following URL:
      http://plaidannotations.googlecode.com/svn/trunk/PlaidAnnotationsUpdateSite/
      Then install Crystal:
      http://crystalsaf.googlecode.com/svn/trunk/EclipseUpdate/
      Then install Plural:
      http://pluralism.googlecode.com/svn/trunk/PluralEclipseUpdate/
      After each plugin is installed, Eclipse will ask if you would like to reset. You only need to
      reset once after all three plugins are installed. For more detailed directions, see the Plural
      wiki:
      http://code.google.com/p/pluralism/wiki/Installation
  3.) Now you can install the Anek plugin. There shouldn't be much to it. The project is checked into
      source depot in the same directory where this README file is found. The subdirectory that contains
      the Eclipse plugin is called "GraphExtractor," for historical reasons. To build and run the plugin,
      run Eclipse, go to the "File" menu and select "Import." Choose to "Import Existing Projects into
      Workspace," which is under the "General" heading. Select the Root Directory which is 
      anek\GraphLoader\ and everything should be fine.
  
  Running the Anek Eclipse plugin:
  1.) To actually run the plugin, you need to create a new "Run Configuration." Go to the "Run" menu, select
      "Run Configurations." Right-click on "Eclipse Application" and select "New." Go to the "Arguments"
      tab of the configuration. Change the "Working Directory" from the eclipse directory to the location
      of the GraphExtractor plugin, something like "C:\...\anek\GraphExtractor\". Once you are done, 
      select "RUn" and a new instance of Eclipse will start up with the Anek plugins included.
  2.) Now to use the plugin you have a few options, but I'll start with the most difficult. ;-)
      Find the Java files you want to run Anek on, select them, right-click, go to the Anek menu and
      choose extract graph. It will prompt you to store the XML representation somewhere. Once you 
      select a location, you are done. You can now run Anek on this XML file.
  3.) After inference is done, select the same Java files, right-click, go to the Anek menu and select the
      option to Apply the graph.
  4.) If you want to run the entire thing inside of Eclipse, you just need to tell Eclipse where the
      executable is located. Go to the "Window" menu and then select preferences. Under the Anek 
      preferences tab, give Eclipse the location of the executable. Probably this will be something
      like "C:\...\anek\GraphLoader\bin\Debug\GraphLoader.exe"

  Installing the Anek Inference Engine:
  1.) There's not much work to be done to install and build the Anek inference engine. Just load
      the solution into Visual Studio 2010. The solution is located under anek\GraphLoader\.
  2.) If you don't have VS2010, you can still build the project, you just may need more help from
      me. :-)
  
  Running the Anek Inference Engine:
  1.) If you're running the anek inference engine externakl to Eclipse, you need to find the exe.
      It will either be anek\GraphLoader\bin\Debug\GraphLoader.exe or 
      anek\GraphLoader\bin\Release\GraphLoader.exe depending on how you built it.
  2.) The engine takes three files arguments, the path to the input graph file, the path to the
      output graph file, and the path to the ConstraintParameters file. I just use the file that's
      checked into SD, at anek\ConstraintParameters.txt. Feel free to tweak the parameters.


That's it! For other questions please email me, nbeckman@cs.cmu.edu
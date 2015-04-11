# plural
[![alt text](http://farm1.static.flickr.com/29/47436435_747cb55015.jpg)](http://www.flickr.com/photos/massenpunkt/47436435/ "machine")

Plural is a sound modular typestate checking tool for Java that employs fractional permissions to allow flexible aliasing control. Plural supports synchronized blocks and atomic blocks for checking concurrent programs. Typestates can be used to define object protocols with finite state machines; Plural's protocols can involve multiple interacting objects. Plural is developed by [Nels Beckman][1] and [Kevin Bierhoff][2] at [Carnegie Mellon University][3] based on their papers published together with [Jonathan Aldrich][4] at [OOPSLA 2007][5] and [2008][6] and elsewhere.

The tool is an Eclipse plugin that relies on the [Crystal static analysis framework][7]. It also uses the [Antlr][8] parser generator, which is released under the BSD license. Some Plural test code comes from the [Jgroups][9] project, which is released under the LPGL v2.1 license.

## Latest News
* We've moved our project to GitHub!
* We started a FAQ page to collect solutions to common issues people run into using Plural. Feel free to "comment" your questions!
* We've added a screen capture demo of Sync-or-Swim to Vimeo here. This has the added benefit of actually having readable text. (Use HD, and watch in fullscreen mode.)
* There is now a demo of Plural on YouTube. As part of Kevin's Thesis, a screen capture, with audio, was recorded. You can check it out here.
* The "Fiddle" object protocol visualization no longer depends on any outside plug-ins, so it should be much easier to install. There will be an updated pushed out soon.
* We wrote a paper on Plural's internals and in particular, how it tracks permissions in Java. Check it out!
* We changed the update site URL to avoid confusion with the Crystal update site, see Installation.
* A new wiki page, on GettingStarted should help the new PLURAL user. Please post any questions as comments to this page, and we'll get back to you ASAP!
* We have finally posted the PLURAL annotations which are needed to run our analysis. Find out more on the Annotations page.
* Nels has posted a series of NIMBY examples that are in the PLURAL test project and discussed on this page.

## Support
This material is based upon work supported by the National Science Foundation under Grant #CCF-0546550 and #CCF-0811592, and DARPA grant #HR00110710019. Any opinions, findings and conclusions or recomendations expressed in this material are those of the author(s) and do not necessarily reflect the views of the National Science Foundation (NSF) or DARPA.

Photo by [massenpunkt](http://www.flickr.com/photos/massenpunkt/47436435/ "massenpunkt").

[1]: http://nelsbeckman.com
[2]: http://www.cs.cmu.edu/~kbierhof/
[3]: http://www.cmu.edu/
[4]: http://www.cs.cmu.edu/~aldrich
[5]: http://www.oopsla.org/oopsla2007/
[6]: http://www.oopsla.org/oopsla2008/
[7]: http://code.google.com/p/crystalsaf/
[8]: http://antlr.org/
[9]: http://www.jgroups.org/

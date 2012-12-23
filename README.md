Aptana Studio With Content Assist Patch
=======================================

What is this?
-------------
This is a modified Aptana Studio 3 with better support for Javascript &amp; jQuery content assist.  I also include
`jquery-1.8.3.sdocml`, a ScriptDoc XML documentation for jQuery 1.8.3 which content taken from api.jquery.com.  The 
ScriptDoc XML is not compatible with official release of Aptana Studio 3.

The modified Aptana Studio 3 is created based on my preferences.  I don't know if it will be useful to others. 
I will just put it here.

Features
--------
*  Content assist support more than one function with the same name but different parameters.  This kind of 
    'polymorphism' can be seen in jQuery API documentation.

![Content assist support polymorphism](https://github.com/JockiHendry/aptanastudio-contentassist-patch/wiki/screenshot1.png)
*  When chaining function call in jQuery, content assist is still working and showing proposals for the new function.

![Content assist still working when chaining function call](https://github.com/JockiHendry/aptanastudio-contentassist-patch/wiki/screenshot2.png)

*  Context info will be displayed for selected jQuery function proposal.  Description for parameters are taken from
    jQuery API documentation.  Context info for some proposals also include example.

![Information for parameters and examples](https://github.com/JockiHendry/aptanastudio-contentassist-patch/wiki/screenshot3.png)

*  jQuery content assist will work properly inside HTML documents.  This is not true in the official relase.

![jQuery content assist works not only in Javascript file but also in HTML file](https://github.com/JockiHendry/aptanastudio-contentassist-patch/wiki/screenshot4.png)

*  Support inline JSDoc for function parameter.

![Support inline JSDoc for function parameter](https://github.com/JockiHendry/aptanastudio-contentassist-patch/wiki/screenshot5.png)

Getting Started
---------------
1.  Download `aptanastudio3_patched.zip` and `jquery-1.8.3.sdocml`.
2.  Extract the zip file to a folder.
3.  Inside `aptana` folder, double click on `AptanaStudio3.exe`.
4.  Wait for Aptana Studio 3 to launch.
5.  Create a new web project by selecting *File*, *New*, *Web Project*.  Enter project name and click *Finish*.
6.  Copy and paste `jquery-1.8.3.sdocml` to the new project. This file can be in anywhere as long as it is inside
    the current project.  Without this file, jQuery content assist will not work properly.
7.  Copy and paste `jquery-x.x.x.js` to the new project.  This is a jQuery script file that can be downloaded from
    jQuery official sites or linked from a CDN.
8.  Create a new file by right clicking the project and selecting *New*, *File*.  Enter file name such as `myscript.js`.
    Click *Finish*.
9.  Start coding!

# AttendanceSystem
<h2>Face recognition based attendance system</h2>

This repository contains three git submodules:-
<ul>
<li>AttendanceSystemTeacherVersion</li>
<li>AttendanceSystemStudentVersion</li>
<li>AttendanceSystemBackend</li>
</ul>

The submodule is it’s own repo/work-area, with its own .git directory.

So, first commit/push your submodule’s changes:

<pre>
<code class="language-html">
$ cd path/to/submodule
$ git add <stuff>
$ git commit -m "comment"
$ git push
</code>
</pre>
<p>Then tell your main project to track the updated version:</p>
<pre>
<code class="language-html">
$ cd /main/project
$ git add path/to/submodule
$ git commit -m "updated my submodule"
$ git push
</code>
</pre>

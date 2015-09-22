Lucene AnalyzingInfixSuggester Bug Demo
=======================================

Currently `AnalyzingInfixSuggester` always opens an index writer, even if the
suggester will be used entirely in read-only mode. I was trying to serve
suggestions out of the same index in a multithreaded setup, but I could only
create one suggester per index per process because of this design.

I've created this GitHub project to demonstrate the bug.

To run, simple run:

    ./gradlew build
    java -jar build/libs/lucene-analyzing-infix-suggester-bug.jar

The exception reads:

    Exception in thread "Thread-1" java.lang.RuntimeException: org.apache.lucene.store.LockObtainFailedException: Lock held by this virtual machine: <INDEX DIR>/write.lock
        at org.lukhnos.lucenestudy.AnalyzingInfixSuggesterBug$1SuggestReader.run(AnalyzingInfixSuggesterBug.java:65)
        at java.lang.Thread.run(Thread.java:745)
    Caused by: org.apache.lucene.store.LockObtainFailedException: Lock held by this virtual machine: <INDEX DIR>/write.lock
        at org.apache.lucene.store.NativeFSLockFactory.obtainFSLock(NativeFSLockFactory.java:127)
        at org.apache.lucene.store.FSLockFactory.obtainLock(FSLockFactory.java:41)
        at org.apache.lucene.store.BaseDirectory.obtainLock(BaseDirectory.java:45)
        at org.apache.lucene.index.IndexWriter.<init>(IndexWriter.java:775)
        at org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester.<init>(AnalyzingInfixSuggester.java:251)
        at org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester.<init>(AnalyzingInfixSuggester.java:163)
        at org.lukhnos.lucenestudy.AnalyzingInfixSuggesterBug$1SuggestReader.run(AnalyzingInfixSuggesterBug.java:51)
        ... 1 more

To work around this problem, I'm currently using my own modified "read only"
`AnalyzingInfixSuggester`, in which I commented out the index writer creation
code in the constructor. Then, in the `lookup` method, I also commented out
the part where we get an `EarlyTerminatingSortingCollector` out of the
index writer, so that only a `TopFieldCollector` is used.

I was wondering if a read-only mode can be added to `AnalyzingInfixSuggester`,
or at least the contract of `getIndexWriterConfig` can be changed – since
one will have to subclass to use a different index writer config anyway –
such that if one returns `null` in `getIndexWriterConfig`, the suggester
will operate in read-only mode, and so no index writer is created. Of course
an error will have to be thrown if any build or update methods are called in
such mode.

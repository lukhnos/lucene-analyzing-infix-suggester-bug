package org.lukhnos.lucenestudy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnalyzingInfixSuggesterBug {
  public static void main(String args[]) throws IOException, InterruptedException {

    // By default, create 10 reader threads.
    int threadCount;
    if (args.length > 0) {
      threadCount = Integer.parseInt(args[0]);
    } else {
      threadCount = 10;
    }

    final Path tmpDir = Files.createTempDirectory(AnalyzingInfixSuggesterBug.class.getName());
    final Analyzer analyzer = new StandardAnalyzer();

    // Populate the data first.
    Directory dir = FSDirectory.open(tmpDir);
    AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(dir, analyzer);
    suggester.add(new BytesRef("Apache Lucene"), null, 0, null);
    suggester.add(new BytesRef("Java language"), null, 0, null);
    suggester.commit();
    suggester.close();

    // Use Runnable to create suggestion reader.
    class SuggestReader implements Runnable {
      int sleepTime = 50 + new Random().nextInt(50);

      @Override
      public void run() {
        Directory dir = null;
        try {
          Thread.sleep(sleepTime);

          dir = FSDirectory.open(tmpDir);
          AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(dir, analyzer);

          List<Lookup.LookupResult> result = suggester.lookup("lucene", false, 10);
          if (result.size() < 0 || !result.get(0).key.equals("Apache Lucene")) {
            throw new AssertionError("Expected to see 'Apache Lucene'");
          } else {
            System.out.println(result.get(0).key);
          }

          // Hold on for a while.
          Thread.sleep(sleepTime);

          suggester.close();
        } catch (IOException|InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    List<Thread> threads = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      threads.add(new Thread(new SuggestReader()));
    }

    for (Thread t : threads) {
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }
  }
}

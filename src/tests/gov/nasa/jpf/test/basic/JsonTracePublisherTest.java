package gov.nasa.jpf.report;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonTracePublisherTest extends TestJPF {

  public static void main(String[] testMethods) {
    runTestsOfThisClass(testMethods);
  }

  @Test
  public void testNormalExecution() throws Exception {
    String jsonFile = "test-normal.json";
    File f = new File(jsonFile);
    if (f.exists()) f.delete();

    if (verifyNoPropertyViolation("+report.publisher=json", "+report.json.file=" + jsonFile)) {
      System.out.println("Normal execution");
    } else {
      assertTrue("JSON file should be created", f.exists());
      String content = new String(Files.readAllBytes(Paths.get(jsonFile)));
      assertTrue("Should contain statistics", content.contains("\"statistics\":"));
      assertTrue("Should contain empty errors array", content.contains("\"errors\": []") || content.replaceAll("\\s", "").contains("\"errors\":[]"));
      f.delete();
    }
  }

  @Test
  public void testAbnormalTermination() throws Exception {
    String jsonFile = "test-abnormal.json";
    File f = new File(jsonFile);
    if (f.exists()) f.delete();

    if (verifyUnhandledException("java.lang.RuntimeException", "+report.publisher=json", "+report.json.file=" + jsonFile)) {
      throw new RuntimeException("Test exception");
    } else {
      assertTrue("JSON file should be created", f.exists());
      String content = new String(Files.readAllBytes(Paths.get(jsonFile)));
      assertTrue("Should contain statistics", content.contains("\"statistics\":"));
      assertTrue("Should contain errors array", content.contains("\"errors\": ["));
      assertTrue("Should contain exception property", content.contains("gov.nasa.jpf.vm.NoUncaughtExceptionsProperty"));
      assertTrue("Should contain trace", content.contains("\"trace\": ["));
      assertTrue("Should contain exception detail", content.contains("Test exception"));
      f.delete();
    }
  }

  @Test
  public void testDeadlock() throws Exception {
    String jsonFile = "test-deadlock.json";
    File f = new File(jsonFile);
    if (f.exists()) f.delete();

    if (verifyDeadlock("+report.publisher=json", "+report.json.file=" + jsonFile)) {
      final Object lock1 = new Object();
      final Object lock2 = new Object();

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          synchronized(lock1) {
            synchronized(lock2) {}
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          synchronized(lock2) {
            synchronized(lock1) {}
          }
        }
      });

      t1.start();
      t2.start();

      try {
        t1.join();
        t2.join();
      } catch (InterruptedException e) {}
    } else {
      assertTrue("JSON file should be created", f.exists());
      String content = new String(Files.readAllBytes(Paths.get(jsonFile)));
      assertTrue("Should contain statistics", content.contains("\"statistics\":"));
      assertTrue("Should contain errors array", content.contains("\"errors\": ["));
      assertTrue("Should contain deadlock property", content.contains("gov.nasa.jpf.vm.NotDeadlockedProperty"));
      assertTrue("Should contain trace", content.contains("\"trace\": ["));
      assertTrue("Should contain thread switch", content.contains("\"thread\": 1") || content.contains("\"thread\": 2"));
      f.delete();
    }
  }
}

package gov.nasa.jpf.report;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Step;
import gov.nasa.jpf.vm.Transition;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

public class JsonTracePublisher extends Publisher {

  public JsonTracePublisher(Config conf, Reporter reporter) {
    super(conf, reporter);
  }

  @Override
  public String getName() {
    return "json";
  }

  @Override
  protected void openChannel() {
    if (out == null) {
      String fname = getReportFileName("report.json.file");
      if (fname == null || fname.equals("report")) {
        fname = "report.json";
      } else if (!fname.endsWith(".json")) {
        fname += ".json";
      }
      try {
        out = new PrintWriter(fname);
      } catch (FileNotFoundException fnfx) {
        // fallback to System.out or ignore
      }
    }
  }

  @Override
  protected void closeChannel() {
    if (out != null) {
      out.close();
      out = null;
    }
  }

  @Override
  public void publishFinished() {
    writeJson();
    super.publishFinished();
  }

  private void writeJson() {
    if (out == null) return;

    out.println("{");
    writeStatistics();
    out.println(",");
    writeErrors();
    out.println("}");
  }

  private void writeStatistics() {
    Statistics stat = reporter.getStatistics();
    out.println("  \"statistics\": {");
    out.println("    \"elapsedTime\": \"" + formatHMS(reporter.getElapsedTime()) + "\",");
    out.println("    \"newStates\": " + stat.newStates + ",");
    out.println("    \"visitedStates\": " + stat.visitedStates + ",");
    out.println("    \"backtrackedStates\": " + stat.backtracked + ",");
    out.println("    \"endStates\": " + stat.endStates + ",");
    out.println("    \"maxMemoryMB\": " + (stat.maxUsed >> 20));
    out.print("  }");
  }

  private void writeErrors() {
    List<Error> errors = reporter.getErrors();
    out.println("  \"errors\": [");
    Iterator<Error> errIt = errors.iterator();
    while (errIt.hasNext()) {
      Error e = errIt.next();
      out.println("    {");
      out.println("      \"id\": " + e.getId() + ",");
      out.println("      \"property\": \"" + escapeJson(e.getProperty().getClass().getName()) + "\",");
      out.println("      \"details\": \"" + escapeJson(e.getDetails()) + "\",");
      
      out.println("      \"trace\": [");
      Path path = e.getPath();
      if (path != null) {
        Iterator<Transition> tIt = path.iterator();
        int tIndex = 0;
        while (tIt.hasNext()) {
          Transition t = tIt.next();
          out.println("        {");
          out.println("          \"transitionId\": " + tIndex + ",");
          out.println("          \"thread\": " + t.getThreadIndex() + ",");
          
          ChoiceGenerator<?> cg = t.getChoiceGenerator();
          if (cg != null) {
            out.println("          \"choiceGenerator\": {");
            out.println("            \"class\": \"" + escapeJson(cg.getClass().getName()) + "\",");
            out.println("            \"choice\": " + cg.getProcessedNumberOfChoices());
            out.println("          },");
          }
          
          out.println("          \"steps\": [");
          Iterator<Step> sIt = t.iterator();
          while (sIt.hasNext()) {
            Step s = sIt.next();
            out.println("            {");
            out.println("              \"source\": \"" + escapeJson(s.getLocationString()) + "\",");
            out.println("              \"instruction\": \"" + escapeJson(s.getInstruction().toString()) + "\"");
            out.print("            }");
            if (sIt.hasNext()) {
              out.println(",");
            } else {
              out.println();
            }
          }
          out.println("          ]");
          out.print("        }");
          if (tIt.hasNext()) {
            out.println(",");
          } else {
            out.println();
          }
          tIndex++;
        }
      }
      out.println("      ]");
      out.print("    }");
      if (errIt.hasNext()) {
        out.println(",");
      } else {
        out.println();
      }
    }
    out.println("  ]");
  }

  private String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\f", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
  }
}

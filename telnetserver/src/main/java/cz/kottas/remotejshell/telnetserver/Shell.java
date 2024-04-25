package cz.kottas.remotejshell.telnetserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class Shell {
    private JShell js;
    private Map<String, Object> result;
    private String cmd;

    Shell() {
        js = jdk.jshell.JShell.builder().executionEngine("local").build();
    }

    Map<String, Object> map() { return new TreeMap<String, Object>(); }

    private void doAutoComplete(String code, int cursor, Map<String, Object> result) throws Exception {
        var objects = new Vector<Object>(5);
        var anchor = new int[1];
        for (var i : js.sourceCodeAnalysis().completionSuggestions(code, cursor, anchor))
            objects.add(i.continuation());

        if (!objects.isEmpty()) {
            result.put("suggest", objects);
            if (anchor[0] != 0)
                result.put("suggestAnchor", anchor[0]);
        }
    }

    private void generateExceptionResult(String code, Throwable t, Map<String, Object> result) {
        var e = map();
        try {
            e.put("info", t.toString());
            doAutoComplete(code, code.length(), result);
        } catch (Throwable t2) {
            e.put("other", t2.toString());
        }
        result.put("error", e);
    }

    private void doEval(String code, Map<String, Object> result) throws Throwable {
        var analyse = js.sourceCodeAnalysis().analyzeCompletion(code);
        var res = js.eval(analyse.source());
        boolean some_error = false;
        var objects = new Vector<Object>(5);

        for (var i : res) {
            if (i.status() != Snippet.Status.VALID) {
                var item = map();
                item.put("value", i.value());
                item.put("status", i.status().toString());
                item.put("info", i.toString());
                objects.add(item);
                some_error = true;
            } else {
                if (res.size() == 1)
                    result.put("res", i.value());
                else
                    objects.add(i.toString());
            }
        }

        if (!objects.isEmpty())
            result.put("res", objects);
        if (some_error)
            doAutoComplete(code, code.length(), result);
    }

    public synchronized byte[] processCommand(String messageContent) {
        Map<String, Object> result = new TreeMap<>();
        try {
            if (messageContent.startsWith("/suggest ")) {
                var cmd = messageContent.replaceFirst("/suggest ", "");
                var cursor = "";
                while (cmd.matches("^[0-9].*")) {
                    cursor += cmd.charAt(0);
                    cmd = cmd.substring(1);
                }
                if (cmd.startsWith(" "))
                    cmd = cmd.substring(1);
                doAutoComplete(cmd, Integer.parseInt(cursor), result);
            } else {
                doEval(messageContent, result);
            }
        } catch (Throwable t) {
            generateExceptionResult(messageContent, t, result);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        var os = new ByteArrayOutputStream();
        try {
            objectMapper.writer().writeValue(os, result);
            os.write('\n');
            return os.toByteArray();
        } catch (Exception e) {
            return ("Unknown exception " +e.toString()).getBytes();
        }
    }

}

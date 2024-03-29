package org.apache.tools.ant.input;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.ReflectUtil;
public class SecureInputHandler extends DefaultInputHandler {
    public SecureInputHandler() {
    }
    public void handleInput(InputRequest request) throws BuildException {
        String prompt = getPrompt(request);
        try {
            Class system = Class.forName("java.lang.System");
            Object console = ReflectUtil.invokeStatic(system, "console");
            do {
                char[] input = (char[]) ReflectUtil.invoke(
                    console, "readPassword", String.class, prompt,
                    Object[].class, (Object[]) null);
                request.setInput(new String(input));
                java.util.Arrays.fill(input, ' ');
            } while (!request.isInputValid());
        } catch (Exception e) {
            super.handleInput(request);
        }
    }
}
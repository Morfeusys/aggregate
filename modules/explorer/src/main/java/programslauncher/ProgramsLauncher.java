package programslauncher;

import com.aggregate.api.Markup;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static programslauncher.Settings.loadFromFile;

public class ProgramsLauncher extends AbstractVerticle{
    private static Logger log = LoggerFactory.getLogger(ProgramsLauncher.class);
    private static final String TEMP_PATH = System.getProperty("java.io.tmpdir");
    private static Robot robot;
    String pathToFile = "";
    private Program[] loadedSettings;
    private Markup responseSpeech;

    @Override
    public void start(/*Future<Void> f*/) throws Exception {

        File settingsPath = new File(TEMP_PATH, "programslauncher");
        File settings = new File(settingsPath, "settings.set");
        // Загруженные настройки
        try {
            loadedSettings = loadFromFile(settings/*, f*/);
            for (int i = 0; i < loadedSettings.length; i++) {
                log.info("loadedPattern: " + loadedSettings[i].getPattern());
            }
        } catch (Exception e) {
            /*f.fail(e);*/
            log.error("CANNOT LOAD FROM FILE !!!");
        }

        /*// Отправляем список шаблонов в агрегат
        vertx.eventBus().consumer("cmd.programslauncher.launchPatterns", m -> {
            try {
                m.reply(getCustomPatterns());
            } catch (Exception e) {
                log.error(e);
            }
        });

        vertx.eventBus().consumer("cmd.programslauncher.launchPattern", m ->{
            responseSpeech = Request.fromMessage(m).markup.get("launchPattern");
            try {
                run(responseSpeech);
            } catch (Exception e) {
                e.printStackTrace();
            }

        });*/

        KeyEvent d = new KeyEvent(new Component() {
            @Override
            public String getName() {
                return super.getName();
            }
        }, 1, 1, 0,10 /*key code*/, KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_UNKNOWN);

        // Запуск окна настройки
        vertx.eventBus().consumer("cmd.programslauncher.settingup", m -> {
            Settings.main(new String[0]);
            /*f.complete();*/
        });
    }

    /**
     * @param type ex.: /launch, /cmd, /key, null
     * @return patternsMap
     * @throws Exception
     */
    public static JsonObject getCustomPatterns(String type) throws Exception {
        File settingsPath = new File(TEMP_PATH, "programslauncher");
        File settings = new File(settingsPath, "settings.set");
        Program[] loadedSettings = loadFromFile(settings/*, f*/);
        Map<String, Object> patternsMap = new HashMap<>();
        int counter1 = 0;

        if (type != null){

            for (int i = 0; i < loadedSettings.length; i++) {
                if (loadedSettings[i].getPath().endsWith("/launch")) {
                    counter1++;
                }
            }

            Program[] lp = new Program[counter1];
            int counter = 0;
            for (int i = 0; i < loadedSettings.length; i++) {
                if (loadedSettings[i].getPath().endsWith(type)) {
                    lp[counter] = loadedSettings[i];
                    counter++;
                }
            }
        }

        for(int i = 0; i < loadedSettings.length; i++) {
          patternsMap.put(loadedSettings[i].getPattern(), i);
        }
        return new JsonObject(patternsMap);
    }

    public static void run(Markup responseSpeech) throws Exception {
        //responseSpeech = Request.fromMessage(m).markup.get("launchPattern");
        log.info("Шаблон номер " + responseSpeech.value);
        File settingsPath = new File(TEMP_PATH, "programslauncher");
        File settings = new File(settingsPath, "settings.set");
        Program loadedProg = loadFromFile(settings)[Integer.parseInt(responseSpeech.value)];

        if (loadedProg.getPath().endsWith("/cmd")){
            executeCmd(loadedProg.getPath().replace("/cmd", ""));// т. к. в имени самого файла не может содержаться slash

            // если "эмуляция нажатий", то получаем keyCode из json и нажимаем
        }else if (loadedProg.getPath().endsWith("/key")){
            try {
                JsonObject keys = new JsonObject(loadedProg.getPath().replace("/key", ""));
                Collection<Object> keyCodes = keys.getMap().values();

                // Нажимаем кнопки
                Robot robot = new Robot();
                robot.keyPress((Integer) keyCodes.toArray()[0]);

                if (keyCodes.toArray().length >= 2) {
                    robot.keyPress((Integer) keyCodes.toArray()[1]);
                }
                if (keyCodes.toArray().length >= 3) {
                    robot.keyPress((Integer) keyCodes.toArray()[2]);
                }

                // Отжимаем кнопки
                robot.keyRelease((Integer) keyCodes.toArray()[0]);
                if (keyCodes.toArray().length >= 2) {
                    robot.keyRelease((Integer) keyCodes.toArray()[1]);
                }
                if (keyCodes.toArray().length >= 3) {
                    robot.keyRelease((Integer) keyCodes.toArray()[2]);
                }

            } catch (AWTException e) {
                log.error(e);
            }
        }else if (loadedProg.getPath().endsWith("/launch")){
            executeCmd("start \"\" \"" + loadedProg.getPath().replace("/launch", "") + "\"");
            log.info("starting " + loadedProg.getPath() + " ...");
        }
    }


    public static void executeCmd(String command/*, Future<Void> f*/){
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        try {
            log.info("cmd command is: " + command);
            builder.start();
        } catch (IOException e) {
           /*f.fail(e);*/
        }
    }

    public static int lineCounter(File file) throws Exception {
        int lineCount = 0;
        String s;
        BufferedReader buf;
        buf = new BufferedReader(new FileReader(file));
        while ((buf.readLine()) != null) {
            lineCount++;
        }
        buf.close();
        return lineCount;
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
    public static void printText(String messageToPrint/*, Future<Void> f*/) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(messageToPrint);
        try{
            clipboard.setContents(stringSelection, (clipboard1, contents) -> {});
        }catch (Exception e) {
            /*f.fail(e);*/
        }

        robot.delay(50);
        pasteFromBuffer();
    }
    public static void copyToBuffer(){
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_C);
        robot.delay(5);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_C);
    }
    public static void pasteFromBuffer(){
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.delay(5);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_V);
    }
    public String getClipboardContents(Future<Void> f) {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText =
                (contents != null) &&
                        contents.isDataFlavorSupported(DataFlavor.stringFlavor)
                ;
        if (hasTransferableText) {
            try {
                result = (String)contents.getTransferData(DataFlavor.stringFlavor);
            }
            catch (Exception e){
                f.fail(e);
            }
        }
        return result;
    }
}
        /*// Блокнот
        vertx.eventBus().consumer("cmd.programslauncher.notepad", m -> {
            executeCmd(loadedSettings[0].getPath()*//*, f*//*);
            *//*f.complete();*//*
        });
        // Paint
        vertx.eventBus().consumer("cmd.programslauncher.paint", m -> {
            executeCmd("start \"\" \"" + loadedSettings[1].getPath() + "\""*//*, f*//*);
            *//*f.complete();*//*
        });
        // Редактор реестра
        vertx.eventBus().consumer("cmd.programslauncher.regedit", m -> {
            executeCmd("start \"\" \"" + loadedSettings[2].getPath() + "\""*//*, f*//*);
            *//*f.complete();*//*
        });
        // Калькулятор
        vertx.eventBus().consumer("cmd.programslauncher.calc", m -> {
            executeCmd("start \"\" \"" + loadedSettings[3].getPath() + "\""*//*, f*//*);
            *//*f.complete();*//*
        });
        vertx.eventBus().consumer("cmd.programslauncher.showwindows", m -> {
            try {
                Robot robot = new Robot();
                robot.keyPress(KeyEvent.VK_WINDOWS);
                robot.keyPress(KeyEvent.VK_TAB);
                robot.delay(10);
                robot.keyRelease(KeyEvent.VK_WINDOWS);
                robot.keyRelease(KeyEvent.VK_TAB);
            } catch (AWTException e) {
                log.error(e);
            }

            *//*f.complete();*//*
        });*/

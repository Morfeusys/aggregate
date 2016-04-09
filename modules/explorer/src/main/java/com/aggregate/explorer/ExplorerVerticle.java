package com.aggregate.explorer;

import com.aggregate.api.Markup;
import com.aggregate.api.Request;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import programslauncher.Program;
import programslauncher.ProgramsLauncher;
import programslauncher.Settings;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

import static java.awt.event.KeyEvent.*;
import static programslauncher.ProgramsLauncher.executeCmd;
import static programslauncher.ProgramsLauncher.getCustomPatterns;
import static programslauncher.ProgramsLauncher.getStackTrace;
import static programslauncher.Settings.loadFromFile;

/**
 * Created by morfeusys on 07.04.16.
 */
public class ExplorerVerticle extends AbstractVerticle {
    private static String PATTERN_LOCAL_APP_NAME = "LocalAppName";
    private static final String PATTERN_REMOTE_APP_NAME = "RemoteAppName";
    private static final String PATTERN_UNNAMED_APP = "UnnamedApp";
    private static final String PATTERN_CUSTOM = "CustomPattern";
    public static final String JSON_URL = "https://raw.githubusercontent.com/Morfeusys/aggregate/master/modules/explorer/apps.json";
    Program[] loadedPrograms = null;
    //JsonObject remote_json
    private static Logger log = LoggerFactory.getLogger(ExplorerVerticle.class);
    private boolean isUnnamedApp = false;
    private boolean isLocal = false;

    private Robot robot;
    private void init() throws Exception {
        int counter1 = 0;
        int counter = 0;
        loadedPrograms = loadFromFile(Settings.settings);

        for (int i = 0; i < loadedPrograms.length; i++) {
            if (loadedPrograms[i].getPath().endsWith("/launch")) {
                counter1++;
            }
        }

        Program[] lp = new Program[counter1];
        for (int i = 0; i < loadedPrograms.length; i++) {
            if (loadedPrograms[i].getPath().endsWith("/launch")) {
                loadedPrograms[i].setPath(loadedPrograms[i].getPath().replace("/launch", ""));
                lp[counter] = loadedPrograms[i];
                counter++;
            }
        }
        loadedPrograms = new Program[lp.length];
        loadedPrograms = lp;
    }

    @Override
    public void start() throws Exception {
        init();
        robot = new Robot();
        robot.setAutoDelay(100);
        vertx.eventBus().consumer("cmd.explorer.app.open", this::runApp);
        vertx.eventBus().consumer("cmd.explorer.close", this::closeApp);
        vertx.eventBus().consumer("cmd.explorer.custom", this::custom);
        vertx.eventBus().consumer("cmd.programslauncher.launchPatterns", m -> {
            try {
                m.reply(getCustomPatterns("/launch"));
            } catch (Exception e) {
                log.error(getStackTrace(e));
            }
        });
        vertx.eventBus().consumer("cmd.programslauncher.settingup", m -> {
            Settings.main(new String[0]);
        });
    }

    private void runApp(Message msg) {
        Request request = Request.fromMessage(msg);
        // TODO: 08.04.2016 если впечатываемый в "appName" текст совпадает со списком из remote json, то нельзя писать шаблон и имена других записей не должны совпадать с json
        Markup app = request.markup.get(PATTERN_REMOTE_APP_NAME);
        if (app == null) {
            app = request.markup.get(PATTERN_LOCAL_APP_NAME);
            isLocal = true;
        }
        if (app == null && isMac()) app = request.markup.get(PATTERN_UNNAMED_APP);
        if (app == null && isWindows()) {
            app = request.markup.get(PATTERN_UNNAMED_APP);
            isUnnamedApp = true;
        }
        openApp(app);
        if (isUnnamedApp){

        }
    }

    private void openApp(Markup app) {
        if (isMac()){
            search(app.value != null && !app.value.isEmpty() && !isUnnamedApp ? app.value : app.source);
        }
        if (isWindows()){
            if (isUnnamedApp){
                search(app.source);
            }else if (isLocal) {
                log.info("isLocal = " + isLocal);
                log.info("app.value = " + app.value);
                if (loadedPrograms[Integer.parseInt(app.value) - 1].getPath() != null || !loadedPrograms[Integer.parseInt(app.value) - 1].getPath().equals("")) {
                    executeCmd("start \"\" \"" + loadedPrograms[Integer.parseInt(app.value) - 1].getPath() + "\"");
                }else log.error("error: loadedPrograms[Integer.parseInt(app.value)].getPath() == null");
            }else {
                boolean isNotFound =  true;
                for (int i = 0; i < loadedPrograms.length; i++) {
                    if (loadedPrograms[i].getName().equals(app.value)) {
                        executeCmd("start \"\" \"" + loadedPrograms[i].getPath() + "\"");
                        isNotFound = false;
                        break;
                    }
                }
                if (isNotFound) search(app.source);
            }

        }
    }
    private void custom(Message msg) {
        Markup responseSpeech = Request.fromMessage(msg).markup.get(PATTERN_CUSTOM);
        try {
            ProgramsLauncher.run(responseSpeech);
        } catch (Exception e) {
            log.error(e);
        }
    }

    private void closeApp(Message msg) {
        if (isMac()) {
            press(VK_META, VK_Q);
        } else {
            press(VK_ALT, VK_F4);
        }
    }

    private void search(String text) {

        if (isMac()) press(VK_META, VK_SPACE);
        else if (isWindows()) press(VK_WINDOWS, VK_S);
        paste(text);
        robot.delay(300);
        press(VK_ENTER);
    }

    private void paste(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(text);
        clipboard.setContents(stringSelection, stringSelection);
        press(isMac() ? VK_META : VK_CONTROL, VK_V);
    }

    private void press(int... keys) {
        for (int key : keys) {
            robot.keyPress(key);
        }
        for (int i = keys.length - 1; i >= 0; i--) {
            robot.keyRelease(keys[i]);
        }
    }

    public String getClipboardContents() {
        String result = null;
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText =
                (contents != null) &&
                        contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String)contents.getTransferData(DataFlavor.stringFlavor);
            }
            catch (UnsupportedFlavorException | IOException e){
                log.error(e);
            }
        }
        return result;
    }


    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}

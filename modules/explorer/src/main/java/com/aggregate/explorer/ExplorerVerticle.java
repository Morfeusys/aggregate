package com.aggregate.explorer;

import com.aggregate.api.Markup;
import com.aggregate.api.Request;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.Collections;

import static java.awt.event.KeyEvent.*;

/**
 * Created by morfeusys on 07.04.16.
 */
public class ExplorerVerticle extends AbstractVerticle {
    private static final String PATTERN_APP_NAME = "AppName";
    private static final String PATTERN_UNNAMED_APP = "UnnamedApp";

    private Robot robot;

    @Override
    public void start() throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);
        vertx.eventBus().consumer("cmd.explorer.app", this::runApp);
        vertx.eventBus().consumer("cmd.explorer.close", this::closeApp);
    }

    private void runApp(Message msg) {
        Request request = Request.fromMessage(msg);
        Markup app = request.markup.get(PATTERN_APP_NAME);
        if (app == null) app = request.markup.get(PATTERN_UNNAMED_APP);
        search(app.value != null ? app.value : app.source);
    }

    private void closeApp(Message msg) {
        if (isMac()) {
            press(VK_META, VK_Q);
        } else {
            press(VK_CONTROL, VK_F4);
        }
    }

    private void search(String text) {
        press(VK_META, isMac() ? VK_SPACE : VK_S);
        paste(text);
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


    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
}

package com.converter.scriptconverter;

import com.aggregate.api.Markup;
import com.aggregate.api.Pattern;
import com.aggregate.api.Request;
import com.aggregate.api.Response;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by AntonPopov on 19.07.16.
 */
public class ScriptConverter extends AbstractVerticle {
    private static Logger log = LoggerFactory.getLogger(ScriptConverter.class);
    private static Map<String, String> vars = new HashMap();
    private boolean isSuspended = false;

    private boolean isDoneCommandAction1;
    private boolean isDoneTtsAction1;
    private boolean isDoneDialogAction1;
    private boolean isDoneIntentAction1;
    private boolean isDoneHttpAction1;
    private boolean isDoneIftttAction1;
    private boolean isDoneStorageAction1;

    @Override
    public void start(Future<Void> f) throws Exception {

        vertx.eventBus().consumer("cmd.GetDialogPattern", m -> {
            log.info("\n\nGetDialogPattern called\n\n");
            m.reply(new JsonObject("{\"контек*|коньки\" : 1, \"пока\" : 2}")); // // fixme unknown error on setting DialogPattern (appeared after adding context)
        });

        vertx.eventBus().consumer("cmd.Pattern1", m -> {
            initVars(m);
            executeActions(m);
        });
        vertx.eventBus().consumer("cmd.Pattern2", m -> {
            initVars(m);
            executeActions(m);
        });

        // ...

        vertx.eventBus().consumer("cmd.DialogCommand", m -> {
            log.info("Pattern №" + Request.fromMessage(m).markup.get("DialogPattern").value);
            if (Request.fromMessage(m).markup.get("DialogPattern").value == null) vars.put("dialog", "0");
            else {
                vars.put("dialog", Request.fromMessage(m).markup.get("DialogPattern").value);
                vars.put("dialog_src", Request.fromMessage(m).markup.source);
            }
            executeActions(m);
        });
        vertx.eventBus().consumer("cmd.DialogContext", m -> {
            //if (Request.fromMessage(m).markup.isBlank())
            //if (Request.fromMessage(m).markup.isEmpty())
            log.info("markup.isBlank() = " + Request.fromMessage(m).markup.isEmpty());
            log.info("markup.isEmpty() = " + Request.fromMessage(m).markup.isBlank());
            scriptCompleted();
        });
        f.complete();
    }

    private void initVars(Message m) {

        vars.put("pattern", m.address().substring(m.address().length() - 1));
        vars.put("src", Request.fromMessage(m).markup.source);
        String[] loc = MicroMethods.getLocation();
        vars.put("loc_lat", loc[0]);
        vars.put("loc_lon", loc[1]);
        vars.put("current_time_ts", String.valueOf(System.currentTimeMillis() / 1000L));
        vars.put("response_text", "");
        vars.put("response_speech", "");
        vars.put("Contact", "");// TODO google contacts api (HTTP requests)
        vars.put("Hello", "дорогой пользователь"); // for testing purposes

        for (Markup markup : Request.fromMessage(m).markup.children) {

            if (markup.name.equals(Pattern.TEXT)) {
                if (markup.source != null || markup.source != "") vars.put(Pattern.TEXT, markup.source);
            }
            if (markup.name.equals(Pattern.NUMBER)) {
                if (markup.value != null || markup.value != "") vars.put(Pattern.NUMBER, markup.value);
            }
            if (markup.name.equals(Pattern.DATE)) {
                if (markup.source != null || markup.source != "") {
                    vars.put(Pattern.DATE, markup.source);
                    vars.put(Pattern.DATE + "_day", String.valueOf((int) markup.data.get("day")));
                    vars.put(Pattern.DATE + "_month", String.valueOf((int) markup.data.get("month")));
                    vars.put(Pattern.DATE + "_year", String.valueOf((int) markup.data.get("year")));
                    vars.put(Pattern.DATE + "_fmt", vars.get(Pattern.DATE + "_day") + "." + vars.get(Pattern.DATE + "_month") + "." + vars.get(Pattern.DATE + "_year")); //// TODO add zero (21.6.2016)
                    vars.put(Pattern.DATE + "_ts", ""); //// TODO need 01.01.2000 to UNIX time converter
                }
            }
            if (markup.name.equals(Pattern.TIME)) {
                if (markup.source != null || markup.source != "") {
                    vars.put(Pattern.TIME, markup.source);
                    vars.put(Pattern.TIME + "_hour", (String) markup.data.get("hour"));
                    vars.put(Pattern.TIME + "_minute", (String) markup.data.get("minute"));
                    vars.put(Pattern.TIME + "_second", (String) markup.data.get("second"));
                    try {
                        vars.put(Pattern.TIME + "_part", markup.data.get("part").equals("0") ? "AM" : "PM");
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void executeActions(Message m) {
        isSuspended = false;
        if (!isDoneCommandAction1 && !isSuspended) executeCommandAction1(m);
        if (!isDoneStorageAction1 && !isSuspended) executeStorageAction1(m);
        if (!isDoneTtsAction1 && !isSuspended) executeTtsAction1(m);
        if (!isDoneDialogAction1 && !isSuspended) executeDialogAction1(m);
        if (!isDoneHttpAction1 && !isSuspended) executeHttpAction1(m);
        if (!isDoneIftttAction1 && !isSuspended) executeIftttAction1(m);
        scriptCompleted();
    }


    private void executeCommandAction1(Message m) {
        String command = "";
        command = variablesSubstitution(command);


        vars.put("response_text", "");
        vars.put("response_speech", "");
        isDoneCommandAction1 = true;
    }

    private void executeTtsAction1(Message m) {
        String reply = "";
        String[] texts = {"Привет $Hello", "Привет привет $Hello"};
        texts = variablesSubstitution(texts);
        reply += texts[(int) (Math.random() * texts.length)];
        say(reply);

        vars.put("response_text", reply); // TODO difference between text & speech
        vars.put("response_speech", reply);
        isDoneTtsAction1 = true;
    }

    private void executeDialogAction1(Message m) {
        String[] patterns = {"привет", "пока"};
        String[] texts = {"1 ответ в диалоге", "2 ответ в диалоге"};
        texts = variablesSubstitution(texts);
        String reply = texts[(int) (Math.random() * texts.length)];

        vertx.eventBus().send("response", new Response(reply, "cmd.DialogContext", true));

        vars.put("response_text", reply);
        vars.put("response_speech", reply);

        // updatePattern(patterns.toJson());
        isDoneDialogAction1 = true;
        suspendActionsExecution();
    }

    private void executeIntentAction1(Message m) {
        isDoneIntentAction1 = true;
    }

    private void executeHttpAction1(Message m) {

        isDoneHttpAction1 = true;
    }

    private void executeIftttAction1(Message m) {

        isDoneIftttAction1 = true;
    }

    private void executeStorageAction1(Message m) {
        String var = "";
        String value = "upper low text";

        value = variablesSubstitution(value);
        value = MicroMethods.evaluateExpression(value);

        vars.put(var, value);
        log.info(value);
        isDoneStorageAction1 = true;
    }

    private void scriptCompleted() {
        isSuspended = false;
        isDoneCommandAction1 = false;
        isDoneTtsAction1 = false;
        isDoneDialogAction1 = false;
        isDoneIntentAction1 = false;
        isDoneHttpAction1 = false;
        isDoneIftttAction1 = false;
        isDoneStorageAction1 = false;
    }

    private String variablesSubstitution(String replacingText) {
        Object[] varsArray = vars.keySet().toArray();

        for (int i = 0; i < vars.size(); i++) {
            replacingText = replacingText.replace("$" + varsArray[i], vars.get(varsArray[i]) == null ? "" : vars.get(varsArray[i]));
        }
        return replacingText;
    }

    private String[] variablesSubstitution(String[] replacingText) {
        Object[] varsArray = vars.keySet().toArray();
        for (int i = 0; i < replacingText.length; i++) {
            for (int j = 0; j < vars.size(); j++) {
                replacingText[i] = replacingText[i].replace("$" + varsArray[j], vars.get(varsArray[i]) == null ? "" : vars.get(varsArray[i]));
            }
        }
        return replacingText;
    }

    private void say(String text) {
        vertx.eventBus().send("response", new Response(text));
    }

    private void suspendActionsExecution() {
        this.isSuspended = true;
    }
}

/* {
    "text": "evaluate expression",
	"vars": [
		{
			"name": "Expression",
			"value": "test_response"
		}
	]
}
*/

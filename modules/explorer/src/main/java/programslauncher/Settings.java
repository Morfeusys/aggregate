package programslauncher;

/**
 * Created by popov on 23.03.2016.
 */

import io.vertx.core.json.JsonObject;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.aggregate.explorer.ExplorerVerticle.JSON_URL;
import static programslauncher.ProgramsLauncher.getStackTrace;

public class Settings extends Application {
    private static final String TEMP_PATH = System.getProperty("java.io.tmpdir");
    public static final File settingsPath = new File(TEMP_PATH, "programslauncher");
    public static final File settings = new File(settingsPath, "settings.set");
    private TableView<Program> table;
    private AutoCompleteTextField addName;
    private TextField addPattern, addPath;
    private Button add = new Button("Добавить");
    private Button delete = new Button("Удалить");
    private Object[] mass;
    private String settingsStr;
    private GridPane grid = new GridPane();
    boolean isEdit = false;
    private ChoiceBox<String> keyChoiceBox1 = new ChoiceBox<>();
    private ChoiceBox<String> keyChoiceBox2 = new ChoiceBox<>();
    private ChoiceBox<String> keyChoiceBox3 = new ChoiceBox<>();
    Button done = new Button("Готово");
    String[][] keys = new String[2][java.awt.event.KeyEvent.class.getDeclaredFields().length];

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage)  {

        settingsPath.mkdirs();
        if (!settingsPath.exists()) {
            System.out.println("Can not create directory for store settings");
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось создать папку для хранения настроек.");
        }
        System.out.println(settingsPath);


        // Название
        TableColumn<Program, String> nameColumn = new TableColumn<>("Название");
        nameColumn.setMinWidth(200);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Шаблон
        TableColumn<Program, String> patternColumn = new TableColumn<>("Шаблон");
        patternColumn.setMinWidth(300);
        patternColumn.setCellValueFactory(new PropertyValueFactory<>("pattern"));

        // Путь к программе
        TableColumn<Program, String> pathColumn = new TableColumn<>("Путь к программе");
        pathColumn.setMinWidth(350);
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));

        // Добавление элементов
        ArrayList<String> list = new ArrayList<>();
        Object[] jsonValues = readJsonFromUrl(JSON_URL).getMap().values().toArray();
        for (int i = 0; i < jsonValues.length; i++) {
            list.add(String.valueOf(jsonValues[i]));
        }
        addName = new AutoCompleteTextField();
        addName.getEntries().addAll(list);
        addPattern = new TextField();
        addPath = new TextField();

        addName.setPromptText("Название");
        addPattern.setPromptText("Шаблон для запуска");
        addPath.setPromptText("Путь к файлу");


        // Выбор типа
        ChoiceBox<String> choiceBox = new ChoiceBox<>();
        choiceBox.getItems().add("Запуск программы");
        choiceBox.getItems().add("Командная строка");
        choiceBox.getItems().add("Эмуляция нажатия клавиш");
        choiceBox.setValue("Запуск программы");
        choiceBox.setTooltip(new Tooltip("Тип действия"));

        choiceBox.setOnAction(event -> {
            choosingProgramType(choiceBox);
        });

        // Кнопки "добавить", "удалить", "готово"
        add.setOnAction(event -> addItem(choiceBox, isEdit));
        delete.setOnAction(event -> {
            Program selectedItem = table.getSelectionModel().getSelectedItem();
            /*if (selectedItem.getName().equals("Paint") ||
                    selectedItem.getName().equals("Блокнот") ||
                    selectedItem.getName().equals("Редактор реестра")||
                    selectedItem.getName().equals("Калькулятор")) {
                showAlert(Alert.AlertType.WARNING, "Предупреждение", "Это неудаляемый элемент");
            }
            else*/ table.getItems().remove(table.getSelectionModel().getSelectedItem());
            table.getSelectionModel().clearSelection();
        });


        done.setOnAction(event -> {
            mass = table.getItems().toArray();

            for (int i = 0; i < mass.length; i++) {
                Program writingProgram = (Program) mass[i];

                if (i == 0) { // Чтобы не было null
                    settingsStr = writingProgram.getName() + "\n" +
                            writingProgram.getPattern() + "\n" +
                            writingProgram.getPath() + "\n";
                } else {
                    settingsStr = settingsStr +
                            writingProgram.getName() + "\n" +
                            writingProgram.getPattern() + "\n" +
                            writingProgram.getPath() + "\n";
                }
            }

            // Пишем в файл
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(settings), "UTF-8"));
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Нейзвестная ошибка: \n" + getStackTrace(e));
            }


            try {
                bw.write(settingsStr);
                bw.close();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Нейзвестная ошибка: \n" + getStackTrace(e));
            }
            primaryStage.close();
        });


        // Панель добавления элементов
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(choiceBox, 0, 0);
        grid.addRow(1, addName, addPattern, addPath, add, delete, done);

        if (!choiceBox.getValue().equals("Командная строка")) {
            // Выбор исполняемого файла
            addPath.setOnMouseClicked(event -> {
                    String sep = System.getProperty("file.separator");
                    File userDir = new File(System.getProperty("user.home"));
                try {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setInitialDirectory(new File(sep +
                            userDir + sep +
                            "AppData" + sep +
                            "Roaming" + sep +
                            "Microsoft" + sep +
                            "Windows" + sep +
                            "Главное меню" + sep +
                            "Программы" + sep));
                    File selectedFile = fileChooser.showOpenDialog(null);

                    addPath.setText(selectedFile.getAbsolutePath());
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Вы не выбрали исполняемый файл !\n" + new File(sep +
                            userDir + sep +
                            "AppData" + sep +
                            "Roaming" + sep +
                            "Microsoft" + sep +
                            "Windows" + sep +
                            "Главное меню" + sep +
                            "Программы" + sep) + "\n\n\n" + getStackTrace(e));
                }
            });
        }

        table = new TableView<>();
        table.setItems(getProgram());
        table.getColumns().addAll(nameColumn, patternColumn, pathColumn);

        table.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
            //Check whether item is selected and set value of selected item to Label
            if (table.getSelectionModel().getSelectedItem() != null) {
                Program selectedProgramm = table.getSelectionModel().getSelectedItem();

                // Выставляем нужный тип функции в choiceBox
                /* По умолчанию */choiceBox.setValue("Запуск программы");
                if (selectedProgramm.getPath().endsWith("/cmd")) choiceBox.setValue("Командная строка");
                if (selectedProgramm.getPath().endsWith("/key"))choiceBox.setValue("Эмуляция нажатия клавиш");

                choosingProgramType(choiceBox);

                if (selectedProgramm.getPath().endsWith("/cmd")) {
                    selectedProgramm.setPath(selectedProgramm.getPath().replace("/cmd", ""));
                }
                addName.setText(selectedProgramm.getName());
                addPattern.setText(selectedProgramm.getPattern());
                addPath.setText(selectedProgramm.getPath());

                if (selectedProgramm.getPath().endsWith("/key")) {
                    JsonObject k = new JsonObject(selectedProgramm.getPath().replace("/key", ""));
                    keyChoiceBox1.setValue((String) k.getMap().keySet().toArray()[0]);
                    keyChoiceBox2.setValue((String) k.getMap().keySet().toArray()[1]);
                    keyChoiceBox3.setValue((String) k.getMap().keySet().toArray()[2]);
                }
            }
        });

        VBox vBox = new VBox(table, grid);

        StackPane root = new StackPane();
        root.getChildren().add(vBox);

        Scene scene = new Scene(root, 850, 250);

        primaryStage.setTitle("Настройки модуля");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void addItem(ChoiceBox<String> choiceBox, boolean isEdit) {
        int indexOfElement1 = 0;
        int indexOfElement2 = 0;
        int indexOfElement3 = 0;
        if (!addName.getText().equals("") && !addPath.getText().equals("")) {

            if (choiceBox.getValue().equals("Эмуляция нажатия клавиш")) {
                if (!addPattern.getText().equals("")) {
                    Map<String, Object> keysMap = new LinkedHashMap<>();
                    // Узнаём код выбранной клавиши
                    for (int i = 0; i < keyChoiceBox1.getItems().size(); i++) {
                        if (keyChoiceBox1.getItems().get(i).equals(keyChoiceBox1.getValue())) indexOfElement1 = i;
                        if (keyChoiceBox2.getItems().get(i).equals(keyChoiceBox2.getValue())) indexOfElement2 = i;
                        if (keyChoiceBox3.getItems().get(i).equals(keyChoiceBox3.getValue())) indexOfElement3 = i;
                    }
                    keysMap.put(keyChoiceBox1.getValue(), Integer.valueOf(keys[0][indexOfElement1]));
                    keysMap.put(keyChoiceBox2.getValue(), Integer.valueOf(keys[0][indexOfElement2]));
                    keysMap.put(keyChoiceBox3.getValue(), Integer.valueOf(keys[0][indexOfElement3]));

                    JsonObject keys = new JsonObject(keysMap);
                    addPath.setText(keys.encode() + "/key");
                    if (keyChoiceBox1.getValue() == null || keyChoiceBox1.getValue().equals("")) {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Введите правильные значения !");
                        return;
                    }
                }else showAlert(Alert.AlertType.ERROR, "Ошибка", "Введите правильные значения !");
            }

            if (choiceBox.getValue().equals("Командная строка")) {
                addPath.setText(addPath.getText() + "/cmd");
            }
            if (choiceBox.getValue().equals("Запуск программы")) {
                Object[] jsonValues = readJsonFromUrl(JSON_URL).getMap().values().toArray();
                for(int i = 0; i < jsonValues.length; i++) {
                    if (addName.getText().equals(jsonValues[i])){
                        addPattern.setText("dGBhKWqAYgO2LM2vyC72nR3YtxPZoC0WTS0i"); // Непроизносимый шаблон
                    }

                }
                addPath.setText(addPath.getText() + "/launch");

            }

            Program program;
            program = new Program(addName.getText(), addPattern.getText(), addPath.getText());

            if (isEdit) {
                table.getItems().remove(table.getSelectionModel().getSelectedIndex());
            }
            table.getItems().add(program);
            addName.clear();
            addPattern.clear();
            addPath.clear();

        }else {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Введите правильные значения !");
        }
    }

    private void choosingProgramType(ChoiceBox<String> choiceBox) {
        // Если выбрано "Командная строка"
        if (choiceBox.getValue().equals("Командная строка")){
            grid.getChildren().clear();
            grid.add(choiceBox, 0, 0);
            grid.addRow(1, addName, addPattern, addPath, add, delete, done);

            TextField tx = (TextField) grid.getChildren().get(3);
            tx.setPromptText("Команда");


            // Если выбрано "Запуск программы"
        } else if (choiceBox.getValue().equals("Запуск программы")){
            grid.getChildren().clear();
            grid.add(choiceBox, 0, 0);
            grid.addRow(1, addName, addPattern, addPath, add, delete, done);

            TextField tx = (TextField) grid.getChildren().get(3);
            tx.setPromptText("Путь к файлу");


            // Если выбрано "Эмуляция нажатия клавиш"
        } else if (choiceBox.getValue().equals("Эмуляция нажатия клавиш")) {
            grid.getChildren().clear();
            grid.add(choiceBox, 0, 0);
            grid.addRow(1, addName, addPattern, keyChoiceBox1, keyChoiceBox2, keyChoiceBox3, add, delete, done);
            // Заносим коды клавиш и их названия в массив
            for (int i = 0; i < KeyEvent.class.getDeclaredFields().length; i++) {
                Field field = KeyEvent.class.getDeclaredFields()[i];
                try {
                    keys[0][i] = String.valueOf(field.getInt(keys));
                    keys[1][i] = KeyEvent.getKeyText(field.getInt(keys));
                } catch (Exception e) {
                    e.getStackTrace();
                }
            }

            // Добавляем кнопки в выпадающее меню
            for (int i = 0; i < keys[1].length; i++) {
                if (keys[1][i] == null) keys[1][i] = "Нет такой клавиши";
                keyChoiceBox1.getItems().add(keys[1][i]);
                keyChoiceBox2.getItems().add(keys[1][i]);
                keyChoiceBox3.getItems().add(keys[1][i]);
            }

        }
    }

    public ObservableList<Program> getProgram() {
        ObservableList<Program> programs = FXCollections.observableArrayList();
        try {
            programs.addAll(loadFromFile(new File(settingsPath, "settings.set")));
        } catch (Exception e) {
            programs.add(new Program("Блокнот", "(откр*|пока*|запуст*) (блокнот|текстов* редактор*)", "notepad.exe"));
            programs.add(new Program("Paint", "(откр*|пока*|запуст*) (графическ* редактор*|редактор* (картин*|изображ*)|Point|Paint)", "mspaint.exe"));
            programs.add(new Program("Редактор реестра", "(откр*|пока*|запуст*) (реестр|редактор* реестр*)", "regedit.exe"));
            programs.add(new Program("Калькулятор", "(откр*|пока*|запуст*) калькулятор*", "calc.exe"));
        }
        return programs;
    }

    public void showAlert(Alert.AlertType alertType, String title, String headerText) {
        try {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(headerText);
            alert.showAndWait();
        } catch (Exception e) {
            System.out.println(getStackTrace(e));
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
    public static Program[] loadFromFile(File file) throws Exception {
        Program[] loadedSettings = new Program[lineCounter(file) / 3];// т.к. строк на одну настройку - 5
        if (file != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));

            // За один проход цикла создаем настройку и ставим ей имя, шаблон и путь
            for (int j = 0; j < loadedSettings.length; j++) {
                try {
                    loadedSettings[j] = new Program(br.readLine(), br.readLine(), br.readLine());
                } catch (Exception e) {}
            }
        }

        /*Program[] finalLoadedSettings = new Program[loadedSettings.length - 1];
        for (int i = 0; i < loadedSettings.length; i++) {
            if ((loadedSettings.length - 1) == i){
                break;
            }
            finalLoadedSettings[i] = new Program(loadedSettings[i].getName(), loadedSettings[i].getPattern(), loadedSettings[i].getPath());
        }*/
        return loadedSettings;
    }

    public static JsonObject readJsonFromUrl(String url)  {
        InputStream is;
        try {
            is = new URL(url).openStream();
        } catch (IOException e) {
            //log.warning(getStackTrace(e));
            return null;
        }
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JsonObject json = new JsonObject(jsonText);
            return json;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                //log.warning(getStackTrace(e));
            }
        }
    }
    private static String readAll(Reader rd){
        StringBuilder sb = new StringBuilder();
        int cp;
        try {
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
        } catch (IOException e) {
            //log.warning(getStackTrace(e));
        }
        return sb.toString();
    }
}
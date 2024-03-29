package main;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import main.genetics.Genome;
import main.genetics.NNGeneticAlgorithm;
import main.mars.Pair;
import main.mars.Player;
import main.mars.SpaceShip;
import main.mars.State;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Controller implements Initializable{

    public static double g = -3.711;
    private int i;
    private ArrayList<Pair> points;
    private Player player;
    private ArrayList<Genome> population;
    private ArrayList<SpaceShip> brains;
    private NNGeneticAlgorithm gen;
    private int generations;
    private ArrayList<Double> averageFitness;
    private ArrayList<HashMap<Integer, Integer>> best;
    private ArrayList<Double> bestFitness; //per gen
    private int numticks;
    private State initialState;
    private double compBest = 0;
    private SpaceShip bestShip;
    private ArrayList<Pair> bestActions;


    @FXML
    private Stage mainStage;

    @FXML
    private Text gen_, bfitness;

    @FXML
    private NumberAxis xAxis, yAxis;

    @FXML
    private LineChart lineChart;

    @FXML
    private ChoiceBox sceneList;

    @FXML
    private Button startButton, runGeneration, printPath, updateScenarioButton, validateSpaceshipsButton;

    @FXML
    private RadioButton toggleScenario1, toggleScenario2, toggleScenario3,toggleScenario4;



    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        xAxis.setLowerBound(0);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(1);
        yAxis.setTickUnit(0.1);

        this.bestActions = new ArrayList<>();
        this.population = new ArrayList<>();
        this.brains = new ArrayList<>();
        this.points = new ArrayList<>();
        this.averageFitness = new ArrayList<>();
        this.bestFitness = new ArrayList<>();
        this.generations = 0;
        this.numticks = 100;
        this.initializeDir("src/main/mars/tc5.txt");
        this.startButton.setOnAction(this::handleStartButtonAction);
        this.runGeneration.setOnAction(this::handleRunButtonAction);
        this.printPath.setOnAction(this::handlePrintButtonAction);
        this.updateScenarioButton.setOnAction(this::handleUpdateScenarioButton);
        this.validateSpaceshipsButton.setOnAction(this::handleValidateSpaceshipsButton);

        addTerrain();
    }



    public void runGeneration(){
        int u = 0;
        this.bestActions = new ArrayList<>();

        System.out.println("Round: " + u);
        for (int i = 0; i < brains.size(); i++) {

            while(!brains.get(i).getEnded()){


                boolean finished = brains.get(i).hasFinished();
                if (!brains.get(i).update()) {
                    System.out.println("ERRROR");
                    break;
                }

                double fitness = 0;
//
                double getDiffVV = 0;
                if(!(Math.abs(brains.get(i).getPlayer().getCurrentState().getvSpeed()) <= Math.abs(Double.valueOf(40))))
                    getDiffVV = Math.abs(Double.valueOf(40) - Math.abs(brains.get(i).getPlayer().getCurrentState().getvSpeed()));
                double getDiffHV = 0;
                if(!(Math.abs(brains.get(i).getPlayer().getCurrentState().gethSpeed()) <= Math.abs(Double.valueOf(20))))
                    getDiffHV = Math.abs(Double.valueOf(20) - Math.abs(brains.get(i).getPlayer().getCurrentState().gethSpeed()));

                double penalty = (getDiffHV + getDiffVV) + 10 *Math.abs(brains.get(i).getPlayer().getCurrentState().getRotate());

                if(brains.get(i).hasLanded()){
                    int pts = getLandingScore(brains.get(i));
                    fitness += (brains.get(i).getPlayer().getCurrentState().getFuel()*10) / penalty;
                    fitness += pts*33.3;
                }
                else {
                    fitness += (brains.get(i).getPlayer().getCurrentState().getFuel())*10 / (brains.get(i).getDistance() + penalty);
                }




                gen.getPopulation().get(i).setFitness(fitness);

                brains.get(i).setFitness(fitness);
                population.get(i).setFitness(fitness);

            }

            if(brains.get(i).getFitness() > compBest){
                compBest = brains.get(i).getFitness();
                bestShip = brains.get(i);
            }
        }

        ArrayList<Pair> output = bestShip.getActions();
        this.bestActions = output;
        System.out.println("CHANGED BEST FITNESS: "  + i + " - " + bestShip.getFitness());
        System.out.println("ENDED GENERATION " + generations);
        gen_.setText((Integer.toString(generations)));
        for (int v = 0; v < brains.size(); v++) {
            System.out.println("brain: " + v + " - " + brains.get(v).getPlayer().getCurrentState());
            System.out.println("fitness: " + brains.get(v).getFitness());System.out.println("CRASHED: " + brains.get(v).hasCrashed());
            int finalV = v;
            Task task = new Task<Void>() {
                @Override public Void call() {

                    XYChart.Series series = new XYChart.Series();
                    series.setName("Attempt " + Integer.toString(finalV+1));

                    for(Pair p: brains.get(finalV).getPath()){

                        series.getData().add(new XYChart.Data((int)p.getKey(), p.getValue()));

                    }

                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            lineChart.getData().add(series);
                        }
                    });

//                            lineChart.getData().setAll(series);


                    return null;
                }
            };

            new Thread(task).start();

        }

        averageFitness.add(gen.getAverageFitness());
        bestFitness.add(gen.getBestFitness());

        System.out.println(gen.getBestFitness());


        System.out.println("BEST FITNESS: " + bestFitness);

        generations++;

        population = gen.Epoch();
        bfitness.setText(Double.toString(gen.getBestFitness()));
        for(int t = 0; t < brains.size(); t++){
            brains.get(t).putWeights(population.get(t).getWeights());
            brains.get(t).reset(initialState);
        }

//        System.out.println("SUPP: " + output);
        compBest = 0;

    }

    public void addTerrain(){
        ArrayList<Pair> terrain = getTerrain();
        System.out.println(terrain);

        Task addTerrain = new Task<Void>() {
            @Override public Void call() {
                XYChart.Series terrainserie = new XYChart.Series();
                terrainserie.setName("Terrain");
                for(int i = 0; i< terrain.size(); i++){

                    terrainserie.getData().add(new XYChart.Data((int)terrain.get(i).getKey(), terrain.get(i).getValue()));
                }

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        lineChart.getData().add(terrainserie);
                    }
                });

                return null;
            }
        };
        new Thread(addTerrain).start();
    }

    private void handleStartButtonAction(ActionEvent actionEvent) {
        int u = 0;
        int x = 0;
        while(x++ != 1500) {
            lineChart.getData().clear();
            addTerrain();
            runGeneration();
        }
    }

    private void handlePrintButtonAction(ActionEvent actionEvent) {
        System.out.println("BEST FITNESS IS : " + gen.getBestFitness());
//        System.out.println("actions: " + this.bestActions);
        List<String> lns = new ArrayList<>();

//        List<String> lines = Arrays.asList(arr, "The second line");
        System.out.println("SIZE: " + this.bestActions.size());

        String arr = new String("ArrayList<Pair> actions = new ArrayList<>();");
        lns.add(arr);
        for(int i = 0; i < bestActions.size(); i++){
            double rotation = (double) bestActions.get(i).getKey();
            double power = (double)bestActions.get(i).getValue();
            String pair = new String("new Pair(" + Integer.valueOf(Double.valueOf(rotation).intValue()) + ", " + Integer.valueOf(Double.valueOf(power).intValue())+")");
            String st = new String("actions.add(" + pair + ");");
            lns.add(st);

        }
        Path file = Paths.get("actions.txt");
        try {
            Files.write(file, lns, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleRunButtonAction(ActionEvent actionEvent) {
        int u = 0;
        int x = 0;
        lineChart.getData().clear();
        addTerrain();
        runGeneration();
    }

    private void handleUpdateScenarioButton(ActionEvent actionEvent) {
        if (toggleScenario1.isSelected()){
            updateScenario(1);
        } else if (toggleScenario2.isSelected()){
            updateScenario(2);
        } else if (toggleScenario3.isSelected()){
            updateScenario(3);
        } else if (toggleScenario4.isSelected()){
            updateScenario(4);
        }
    }

    public void handleValidateSpaceshipsButton(ActionEvent actionEvent){

    }

    public void initializeDir(String dir){

        Scanner in = null;

        try {
            File file = new File(dir);
            in = new Scanner(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int surfaceN = in.nextInt(); // the number of points used to draw the surface of Mars.
        for (int i = 0; i < surfaceN; i++) {

            int landX = in.nextInt(); // X coordinate of a surface point. (0 to 6999)
            int landY = in.nextInt(); // Y coordinate of a surface point. By linking all the points together in a sequential fashion, you form the surface of Mars.
            Pair p = new Pair(landX, landY);
            points.add(p);
        }

        this.player = new Player(points);
        this.initialState = new State(in.nextInt(), in.nextInt(), in.nextDouble(), in.nextDouble(), in.nextInt(), in.nextDouble(), in.nextDouble());
        System.out.println(initialState.gethSpeed());
        this.player.setCurrentState(initialState);
        System.out.println("landing zone: " + this.player.getLandingZone());

        for(int i = 0; i < 16; i++){
            Player p = new Player(points);
            p.setCurrentState(initialState);
            brains.add(new SpaceShip(p));
        }
        this.gen = new NNGeneticAlgorithm(16, brains.get(0).getNumberOfWeights());

        population = gen.getPopulation();

        for(int x = 0; x < brains.size(); x++){
            brains.get(i).putWeights(population.get(i).getWeights());
        }

        System.out.println(surfaceN + " - " + points);
    }

    public void updateScenario(int scenario){
        Scanner in = null;
        String dir = "";
        System.out.println("chosen: " + scenario);
        switch (scenario){
            case 1:
                dir = "src/main/mars/tc1.txt";
                break;
            case 2:
                dir = "src/main/mars/tc2.txt";
                break;
            case 3:
                dir = "src/main/mars/tc3.txt";
                break;
            case 4:
                dir = "src/main/mars/tc4.txt";
                break;
        }

        try {
            File file = new File(dir);
            in = new Scanner(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        points.clear();

        int surfaceN = in.nextInt(); // the number of points used to draw the surface of Mars.
        for (int j = 0; j < surfaceN; j++) {
            int landX = in.nextInt(); // X coordinate of a surface point. (0 to 6999)
            int landY = in.nextInt(); // Y coordinate of a surface point. By linking all the points together in a sequential fashion, you form the surface of Mars.
            Pair p = new Pair(landX, landY);
            points.add(p);
        }

        System.out.println("new terrain: " + points);

        this.initialState = new State(in.nextInt(), in.nextInt(), in.nextDouble(), in.nextDouble(), in.nextInt(), in.nextDouble(), in.nextDouble());
        for(SpaceShip spc: brains){
            spc.getPlayer().setPoints(points);
            spc.getPlayer().setCurrentState(initialState);
        }

        updateTerrain(scenario);

    }

    public void updateTerrain(int scenario){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                lineChart.getData().clear();
            }
        });
        addTerrain();
    }


    public ArrayList<HashMap<Integer,Integer>> getBest(){
        return best;
    }

    public ArrayList<Pair> getTerrain(){
        return points;
    }

    public void start(){


    }

    public void setStage(Stage stage)
    {
        this.mainStage = stage;
    }

    public int getLandingScore(SpaceShip sc) {
        int i = 0;
        if(sc.isLandingHSpeed()) {
            i++;
        }
        if(sc.isLandingVSpeed()) {
            i++;
        }
        if(sc.isLandingRotation()) {
            i++;
        }

        return i;
    }
}

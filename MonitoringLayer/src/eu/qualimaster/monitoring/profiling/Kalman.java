package eu.qualimaster.monitoring.profiling;
import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Kalman Implementation for the QualiMaster-Project using the
 * apache-math-library to reengineer the approach used in the RainMon-Project,
 * which is written in Python.
 * 
 * @author Christopher Voges
 */
public class Kalman {

    /**
     * Noise is eliminated  beforehand, but it can (for
     * algorithm reasons) not be 0. Instead a double value of 0.0001d is set.
     */
    private double measurementNoise = 0.0001d;
    
    /** 
     * The discrete time interval between measurements.
     */
    private double dt = 1d;
    
    /**
     * A - state transition matrix TODO optimal?
     */
    private RealMatrix mA = MatrixUtils.createRealMatrix(new double[][] {
        {1, dt, 0,  0 },
        {0,  1, 0,  0 },
        {0,  0, 1, dt},
        {0,  0, 0,  1 }});

    /**
     * B - control input matrix D TODO optimal?
     */
    private RealMatrix mB = MatrixUtils.createRealMatrix(new double[][] {
        {0, 0, 0, 0 },
        {0, 0, 0, 0 },
        {0, 0, 1, 0 },
        {0, 0, 0, 1 }});
    
    /**
     * H - measurement matrix. 
     */
    private RealMatrix mH = MatrixUtils.createRealMatrix(new double[][] {
        {1, 0, 0, 0 },
        {0, 0, 0, 0 },
        {0, 0, 1, 0 },
        {0, 0, 0, 0 }});

    /**
     * Q - process noise covariance matrix TODO optimal?
     */
    private RealMatrix mQ = MatrixUtils.createRealMatrix(4, 4);
    
    private double var = measurementNoise * measurementNoise;
    /**
     * R - measurement noise covariance matrix  TODO optimal?
     */
    private RealMatrix mR = MatrixUtils.createRealMatrix(new double[][] {
        {var,    0,   0,    0 },
        {0,   1e-3,   0,    0 },
        {0,      0, var,    0 },
        {0,      0,   0, 1e-3 }});

    /**
     * P - error covariance matrix  TODO optimal?
     */
    private RealMatrix mP = MatrixUtils.createRealMatrix(new double[][] {
            {var,    0,   0,    0 },
            {0,   1e-3,   0,    0 },
            {0,      0, var,    0 },
            {0,      0,   0, 1e-3 }});

    /**
     * Vector used to store the start value for a new timeline.
     */
    private RealVector x = MatrixUtils.createRealVector(new double[] {0, 1, 0, 0 });

    /**
     * The {@link ProcessModel} for the Kalman-Filter. TODO Is default good
     * enough?
     */
    private ProcessModel pm = new DefaultProcessModel(mA, mB, mQ, x, mP);

    /**
     * The {@link MeasurementModel} for the Kalman-Filter. TODO Is default good
     * enough?
     */
    private MeasurementModel mm = new DefaultMeasurementModel(mH, mR);

    /**
     * The instance of Apaches {@link KalmanFilter}.
     */
    private KalmanFilter filter = new KalmanFilter(pm, mm);

    /**
     * This vector is used to add e.g. acceleration. For us/now it is a 
     * zero-vector.
     */
    private RealVector controlVector = MatrixUtils.createRealVector(new double[] {0, 0, 0, 0});
    
    /**
     * Default contructor used for a new timeline. TODO Custom constructors to
     * continue partly predicted timelines follows at a later date.
     */
    public Kalman() {
    }
    
    /**
     * This method predicts the value of a timeline one timestep ahead of the
     * given value.
     * 
     * @param xMeasured Timestep of measurement. For now the filter has to be 
     * used iterative, so this value has to be exactly one greater than the 
     * last time this method was called.
     * @param yMeasured Current measurement.
     * @return Prediction for one timestep ahead as {@link Double}.
     */
    public double predict(double xMeasured, double yMeasured) {
        filter.correct(new double[] {xMeasured, 0, yMeasured, 0 });
        filter.predict(controlVector);
        return filter.getStateEstimation()[2];
    }

    /**
     * A short test.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        Kalman kalman = new Kalman();
        
        @SuppressWarnings("unused")
        double[] outputs = {0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000, 0.000000000000000,
            0.063159224404724, 0.093319853798896, 0.091837203251037, 0.090452715842793, 0.089175571838354,
            0.087778330456155, 0.086538877590758, 0.170872016859372, 0.252330553024462, 0.276636651589969,
            65.519171182484500, 4358.799601303840000, 6069.939269910030000, 11088.437069587400000,
            13420.549025692700000, 16257.725484164500000, 17969.888804649900000, 19482.567550740800000,
            23535.593011811000000, 24290.176291793300000, 25930.763877222800000, 26880.213459828000000,
            30623.047081023400000, 33482.558274651900000, 36064.157898954700000, 39232.238272052100000,
            41067.467072842900000, 44707.159143736300000, 49663.088483361400000, 51664.873716287200000,
            56197.071685817100000, 57868.539123240600000, 59030.741904631700000, 62625.922657669300000,
            65127.764935531900000, 67018.885212522200000, 69592.862903225800000, 71238.810136172400000,
            71674.223954991800000, 73897.934029815700000, 76958.663531597400000, 79891.571928814700000,
            79884.289674552800000, 82459.345847126000000, 83050.850615834600000, 84945.973449280700000,
            87510.437161970400000, 88866.556612758300000, 88581.723538576500000, 91111.171477684600000,
            92460.493925196500000, 94422.642004460100000, 96602.900887779100000, 98447.607583508800000,
            97913.730503707500000, 100015.499683076000000, 100459.889552764000000, 102685.576875072000000,
            103253.536102868000000, 104838.372569928000000, 106352.149511456000000, 108158.176232145000000,
            108993.848142474000000, 110460.826131768000000, 111278.784832396000000, 112308.719547846000000,
            112659.380072659000000 };
        double[] latency = {0.000000000000000, 0.000000000000000, 0.000000000000000, 0.002136752136752,
            0.001053740779768, 0.008322824716267, 0.004991192014093, 0.007432615927762, 0.016439483146608,
            0.018139928818003, 0.013611915181012, 0.010915131819024, 0.007745790504053, 0.009848904336709,
            0.012440952243102, 0.012761074521577, 0.012659990101911, 1.729071931488100, 0.381735244428261,
            0.816244156910453, 0.754103934218540, 0.829462771513881, 0.893134969091705, 0.897260148015578,
            0.899386573355938, 0.937654911975257, 0.964299143540760, 0.982206756941997, 1.006475632289390,
            0.972377858708936, 0.898230040173907, 0.843799975136753, 0.834925733703417, 0.831696148309011,
            0.824447119227204, 0.818257322438846, 0.817521483163516, 0.824456978427184, 0.817649342711300,
            0.817649342711300, 0.817649342711300, 0.807690057820104, 0.812967417245450, 0.813241475701387,
            0.813241475701387, 0.821276028831078, 0.822794583829562, 0.820629675297973, 0.825740216512841,
            0.815363449285620, 0.815363449285620, 0.881380300852117, 0.888854172060656, 0.875647223489499,
            0.875104710747552, 0.882715975317315, 0.878999679679394, 0.874514891979857, 0.889562776122216,
            0.893197629084519, 0.890471621140536, 0.889610843101433, 0.902558097625989, 0.916899644995635,
            0.915771844145121, 0.912463706618767, 0.914658936046048, 0.905924460605131, 0.902418568817022,
            0.903766212604831, 0.898687516591928, 0.889974925134923, 1.001787458302470, 1.002220040642630,
            1.000362758107320, 0.995752643027812, 0.994270148241884, 1.019129868326560, 1.019860626589110,
            1.049588174632170, 1.033039341470880, 1.079084511026600, 1.076914603531180, 1.074229055454440,
            1.098142020732010, 1.094205563557560, 1.090611032348660, 1.086576990231720, 1.084917814655110,
            1.082226166535150, 1.078046123703080, 1.074501947961250, 1.073031305896840, 1.070429171575750,
            1.114462852324460, 1.114849749236980, 1.113606474153130, 1.111873122343190, 1.111382617783750,
            1.109339447944880, 1.120126803239300, 1.117649245345650, 1.114958661329030, 1.139123937229450,
            1.183953145176790, 1.183186807186540, 1.220850258655210, 1.218240541773420, 1.223104326738350,
            1.238991353294640, 1.237387667188470, 1.269173507212000, 1.265680016829620, 1.265850066862980,
            1.261588510334210, 1.296830947901800, 1.294941486911870, 1.293891033939110, 1.303390418744930,
            1.300805980717080, 1.296841549694130 };
        double[] inputs = latency;
        
        for (int i = 0; i < inputs.length; i++) {
            System.out.println(i + "\t" + inputs[i] + "\t" + kalman.predict(i, inputs[i]));
        }
    }
    
}

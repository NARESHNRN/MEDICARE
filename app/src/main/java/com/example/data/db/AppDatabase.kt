package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        MedicineScanEntity::class,
        CaregiverEntity::class,
        EmergencyContactEntity::class,
        DrugInteractionEntity::class,
        VerifiedMedicineEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun medicineScanDao(): MedicineScanDao
    abstract fun caregiverDao(): CaregiverDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun drugInteractionDao(): DrugInteractionDao
    abstract fun verifiedMedicineDao(): VerifiedMedicineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medivoice_database.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialKnowledgeGraph(database)
                    }
                }
            }
        }

        private suspend fun populateInitialKnowledgeGraph(database: AppDatabase) {
            val verifiedDao = database.verifiedMedicineDao()
            val interactionDao = database.drugInteractionDao()

            val initialMedicines = listOf(
                VerifiedMedicineEntity(
                    id = "med_1",
                    brandName = "Dolo 650",
                    genericName = "Paracetamol",
                    standardStrength = "650 mg",
                    manufacturer = "Micro Labs Ltd",
                    commonUses = "Mild to moderate pain relief, fever reduction, headache, joint ache",
                    warnings = "Do not exceed 4000 mg in 24 hours. Overdose causes severe liver damage.",
                    agePrecautions = "Adults & elderly: 1 tablet every 6 hours as needed. Not recommended under 12 years without pediatrician advice.",
                    pregnancyStatus = "Considered safe during pregnancy when taken at lowest effective therapeutic doses for shortest duration.",
                    commonSideEffects = "Nausea, mild indigestion, sweating",
                    seriousSideEffects = "Liver toxicity, yellowing of skin or eyes, dark urine, severe skin rash",
                    storage = "Store below 30°C in a dry place protected from direct sunlight",
                    scheduleCategory = "OTC / General Sale"
                ),
                VerifiedMedicineEntity(
                    id = "med_2",
                    brandName = "Crocin 500",
                    genericName = "Paracetamol",
                    standardStrength = "500 mg",
                    manufacturer = "GSK Pharmaceuticals",
                    commonUses = "Relief of fever and aches, headache, toothache, muscle aches",
                    warnings = "Avoid taking with other paracetamol-containing cough or cold preparations.",
                    agePrecautions = "Adults and elderly: 1-2 tablets every 4-6 hours. Maximum 8 tablets daily.",
                    pregnancyStatus = "Generally compatible with pregnancy and breastfeeding at recommended therapeutic doses.",
                    commonSideEffects = "Mild stomach upset",
                    seriousSideEffects = "Severe allergic reaction, breathing difficulty, liver enzyme elevation",
                    storage = "Store in original strip below 25°C away from moisture",
                    scheduleCategory = "OTC"
                ),
                VerifiedMedicineEntity(
                    id = "med_3",
                    brandName = "Augmentin 625 Duo",
                    genericName = "Amoxicillin and Potassium Clavulanate",
                    standardStrength = "625 mg (500mg + 125mg)",
                    manufacturer = "GlaxoSmithKline",
                    commonUses = "Bacterial infections of lungs, ears, sinuses, skin, and urinary tract",
                    warnings = "Complete full prescribed antibiotic course. Never stop early without doctor consultation.",
                    agePrecautions = "Dose adjustment required in renal impairment; close monitoring in elderly patients.",
                    pregnancyStatus = "Category B: Generally safe in pregnancy if prescribed by physician; small amounts enter breast milk.",
                    commonSideEffects = "Diarrhea, nausea, stomach cramps, mild skin rash",
                    seriousSideEffects = "Severe persistent watery diarrhea (C. diff), facial swelling, jaundice, severe skin blistering",
                    storage = "Store in a cool dry place below 25°C. Keep desiccant in foil pack.",
                    scheduleCategory = "Schedule H1 (Prescription Only)"
                ),
                VerifiedMedicineEntity(
                    id = "med_4",
                    brandName = "Glycomet 500",
                    genericName = "Metformin Hydrochloride",
                    standardStrength = "500 mg",
                    manufacturer = "USV Ltd",
                    commonUses = "Management of type 2 diabetes mellitus to lower blood glucose levels",
                    warnings = "Take with or after meals to minimize stomach upset. Avoid excessive alcohol consumption.",
                    agePrecautions = "Elderly users must monitor kidney function regularly (eGFR). Higher risk of lactic acidosis.",
                    pregnancyStatus = "May be prescribed during pregnancy under strict obstetric endocrinology supervision.",
                    commonSideEffects = "Metallic taste, stomach ache, bloating, loose stools",
                    seriousSideEffects = "Lactic acidosis (deep rapid breathing, severe fatigue, severe muscle weakness), hypoglycemia",
                    storage = "Store below 30°C in a dry location",
                    scheduleCategory = "Schedule H (Prescription Only)"
                ),
                VerifiedMedicineEntity(
                    id = "med_5",
                    brandName = "Pan 40",
                    genericName = "Pantoprazole Gastro-resistant",
                    standardStrength = "40 mg",
                    manufacturer = "Alkem Laboratories",
                    commonUses = "Gastroesophageal reflux disease (GERD), acid peptic disease, stomach ulcers",
                    warnings = "Swallow whole with water 30 minutes before morning breakfast. Do not crush or chew.",
                    agePrecautions = "Safe for older adults; long-term use (>1 year) requires monitoring bone mineral density and magnesium levels.",
                    pregnancyStatus = "Category B: Use during pregnancy only if clearly needed and recommended by healthcare provider.",
                    commonSideEffects = "Headache, flatulence, dry mouth, mild nausea",
                    seriousSideEffects = "Severe bone fractures on extended high-dose therapy, hypomagnesemia, Clostridium difficile diarrhea",
                    storage = "Store protected from moisture and light below 25°C",
                    scheduleCategory = "Schedule H (Prescription Only)"
                ),
                VerifiedMedicineEntity(
                    id = "med_6",
                    brandName = "Atorva 10",
                    genericName = "Atorvastatin",
                    standardStrength = "10 mg",
                    manufacturer = "Zydus Cadila",
                    commonUses = "Lowering LDL cholesterol, reducing risk of heart attacks and cardiovascular events",
                    warnings = "Usually taken at bedtime. Report unexplained muscle soreness or tenderness promptly.",
                    agePrecautions = "Commonly prescribed in elderly. Monitor liver enzymes and muscle weakness.",
                    pregnancyStatus = "CONTRAINDICATED in pregnancy and breastfeeding. Can cause fetal harm.",
                    commonSideEffects = "Joint pain, mild headache, indigestion, mild fatigue",
                    seriousSideEffects = "Rhabdomyolysis (severe muscle breakdown), dark tea-colored urine, liver dysfunction",
                    storage = "Store at room temperature between 20°C and 25°C",
                    scheduleCategory = "Schedule H (Prescription Only)"
                ),
                VerifiedMedicineEntity(
                    id = "med_7",
                    brandName = "Ecosprin 75",
                    genericName = "Aspirin (Acetylsalicylic Acid)",
                    standardStrength = "75 mg",
                    manufacturer = "USV Ltd",
                    commonUses = "Antiplatelet blood thinner for prevention of stroke, heart attack, and angina",
                    warnings = "Increases bleeding risk. Do not combine with other NSAID pain relievers without physician guidance.",
                    agePrecautions = "Elderly have increased risk of gastrointestinal bleeding; often co-prescribed with gastroprotective agent.",
                    pregnancyStatus = "Low dose may be prescribed for pre-eclampsia prevention; high dose contraindicated in 3rd trimester.",
                    commonSideEffects = "Heartburn, minor nosebleeds, easy bruising",
                    seriousSideEffects = "Black tarry stools, coffee-ground vomiting, active gastrointestinal hemorrhage",
                    storage = "Store below 25°C in a dry place. Keep container tightly sealed.",
                    scheduleCategory = "Schedule H (Prescription Only)"
                ),
                VerifiedMedicineEntity(
                    id = "med_8",
                    brandName = "Cetzine 10",
                    genericName = "Cetirizine Hydrochloride",
                    standardStrength = "10 mg",
                    manufacturer = "Dr. Reddy's Laboratories",
                    commonUses = "Allergic rhinitis, hay fever, sneezing, runny nose, itchy skin hives (urticaria)",
                    warnings = "May cause mild drowsiness. Avoid operating heavy machinery or driving after taking.",
                    agePrecautions = "Elderly users may be more sensitive to anticholinergic effects and sedation; 5 mg daily often sufficient.",
                    pregnancyStatus = "Category B: Safe in pregnancy when benefit outweighs minimal risk. Consult physician.",
                    commonSideEffects = "Drowsiness, dry mouth, tiredness, mild headache",
                    seriousSideEffects = "Difficulty urinating, severe allergic rash, rapid heartbeat",
                    storage = "Store at controlled room temperature away from heat",
                    scheduleCategory = "Schedule H"
                ),
                VerifiedMedicineEntity(
                    id = "med_9",
                    brandName = "Azee 500",
                    genericName = "Azithromycin",
                    standardStrength = "500 mg",
                    manufacturer = "Cipla Ltd",
                    commonUses = "Respiratory tract infections, throat and tonsil infections, skin infections",
                    warnings = "Take once daily 1 hour before or 2 hours after a meal for full 3 to 5 day course.",
                    agePrecautions = "Use with caution in elderly patients with pre-existing heart rhythm conditions (QT prolongation).",
                    pregnancyStatus = "Category B: Widely used when indicated for bacterial infection during pregnancy.",
                    commonSideEffects = "Nausea, diarrhea, abdominal discomfort, taste alterations",
                    seriousSideEffects = "Cardiac arrhythmia, irregular pulse, severe allergic swelling, cholestatic jaundice",
                    storage = "Store below 30°C in moisture-proof packaging",
                    scheduleCategory = "Schedule H1 (Prescription Only)"
                ),
                VerifiedMedicineEntity(
                    id = "med_10",
                    brandName = "Combiflam",
                    genericName = "Ibuprofen and Paracetamol",
                    standardStrength = "400 mg + 325 mg",
                    manufacturer = "Sanofi India",
                    commonUses = "Relief of severe headaches, toothache, muscular pain, menstrual pain, fever",
                    warnings = "Always take with food or milk to prevent stomach ulceration. Avoid alcohol.",
                    agePrecautions = "Caution in elderly due to cardiovascular and renal fluid retention risks.",
                    pregnancyStatus = "AVOID in pregnancy, especially third trimester (risk of premature closure of ductus arteriosus).",
                    commonSideEffects = "Heartburn, nausea, abdominal discomfort",
                    seriousSideEffects = "Stomach bleeding, fluid retention, kidney impairment, severe allergic reaction",
                    storage = "Store in cool dry place below 25°C",
                    scheduleCategory = "Schedule H"
                ),
                VerifiedMedicineEntity(
                    id = "med_11",
                    brandName = "Amlong 5",
                    genericName = "Amlodipine Besylate",
                    standardStrength = "5 mg",
                    manufacturer = "Micro Labs",
                    commonUses = "Hypertension (high blood pressure) management, chronic stable angina",
                    warnings = "Do not discontinue abruptly. Monitor blood pressure regularly.",
                    agePrecautions = "Initial dose of 2.5 mg recommended in elderly patients due to slower clearance.",
                    pregnancyStatus = "Category C: Use only if potential benefit justifies potential risk to fetus.",
                    commonSideEffects = "Swelling in ankles/feet (peripheral edema), flushing, dizziness",
                    seriousSideEffects = "Marked hypotension, fainting, worsening chest pain on initiation",
                    storage = "Store protected from light and moisture",
                    scheduleCategory = "Schedule H"
                ),
                VerifiedMedicineEntity(
                    id = "med_12",
                    brandName = "Telma 40",
                    genericName = "Telmisartan",
                    standardStrength = "40 mg",
                    manufacturer = "Glenmark Pharmaceuticals",
                    commonUses = "Treatment of hypertension to lower cardiovascular event risks",
                    warnings = "Contraindicated with potassium supplements or salt substitutes without clinical monitoring.",
                    agePrecautions = "Elderly users should check kidney profile and potassium regularly.",
                    pregnancyStatus = "STRICTLY CONTRAINDICATED in 2nd and 3rd trimesters of pregnancy. Can cause fetal death.",
                    commonSideEffects = "Dizziness, back pain, sinus congestion",
                    seriousSideEffects = "Hyperkalemia (high blood potassium), angioedema, acute renal impairment",
                    storage = "Keep in blister foil until immediate use to protect from humidity",
                    scheduleCategory = "Schedule H"
                )
            )
            verifiedDao.insertAll(initialMedicines)

            val initialInteractions = listOf(
                DrugInteractionEntity(
                    drugA = "Aspirin",
                    drugB = "Ibuprofen",
                    severity = "HIGH",
                    warningMessage = "Combining Aspirin with Ibuprofen significantly increases the risk of severe stomach bleeding and ulcers, and Ibuprofen interferes with Aspirin's cardioprotective antiplatelet effect.",
                    clinicalAction = "Avoid simultaneous use. Consult doctor for gastro-safe alternatives."
                ),
                DrugInteractionEntity(
                    drugA = "Ecosprin 75",
                    drugB = "Combiflam",
                    severity = "HIGH",
                    warningMessage = "Ecosprin (Aspirin) and Combiflam (Ibuprofen + Paracetamol) both impair platelet aggregation and cause stomach lining erosion, creating high gastrointestinal bleeding risk.",
                    clinicalAction = "Do not take both together. Consult your healthcare provider immediately."
                ),
                DrugInteractionEntity(
                    drugA = "Atorvastatin",
                    drugB = "Clarithromycin",
                    severity = "HIGH",
                    warningMessage = "Clarithromycin strongly blocks the hepatic breakdown of Atorvastatin, causing toxic statin accumulation and severe muscle breakdown (rhabdomyolysis).",
                    clinicalAction = "Atorvastatin must be temporarily held while taking macrolide antibiotics."
                ),
                DrugInteractionEntity(
                    drugA = "Atorva 10",
                    drugB = "Azithromycin",
                    severity = "MODERATE",
                    warningMessage = "Potential increased risk of statin blood concentration and muscle aches.",
                    clinicalAction = "Monitor for unexplained muscle soreness or weakness."
                ),
                DrugInteractionEntity(
                    drugA = "Metformin",
                    drugB = "Alcohol",
                    severity = "HIGH",
                    warningMessage = "Combining Metformin with alcohol sharply escalates the risk of life-threatening Lactic Acidosis and severe hypoglycemia.",
                    clinicalAction = "Do not consume alcohol while undergoing Metformin therapy."
                ),
                DrugInteractionEntity(
                    drugA = "Dolo 650",
                    drugB = "Crocin 500",
                    severity = "HIGH",
                    warningMessage = "Both medicines contain Paracetamol. Taking both causes dangerous accidental overdose and acute liver toxicity.",
                    clinicalAction = "Never combine multiple paracetamol brands. Choose only one as prescribed."
                ),
                DrugInteractionEntity(
                    drugA = "Paracetamol",
                    drugB = "Combiflam",
                    severity = "HIGH",
                    warningMessage = "Combiflam already contains Paracetamol (325mg). Adding Paracetamol risks exceeding safe daily hepatic limits.",
                    clinicalAction = "Do not take additional paracetamol while taking Combiflam."
                ),
                DrugInteractionEntity(
                    drugA = "Amlodipine",
                    drugB = "Telmisartan",
                    severity = "MODERATE",
                    warningMessage = "Both are blood pressure lowering medications. Combined use can cause excessive drop in blood pressure (hypotension) and dizziness.",
                    clinicalAction = "Take only under doctor's guidance; rise slowly from sitting or lying position."
                )
            )
            interactionDao.insertAll(initialInteractions)
        }
    }
}

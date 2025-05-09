# Seminární práce: Paralelní simulace SIR modelu

## 1. Úvod do problematiky – SIR model

SIR model je jedním ze základních matematických modelů používaných v epidemiologii k popisu šíření infekčních onemocnění v populaci. Název modelu je odvozen od tří skupin, do kterých jsou jedinci v populaci rozděleni:

* **S (Susceptible) – Náchylní:** Jedinci, kteří jsou zdraví, ale mohou se nakazit.
* **I (Infected) – Infikovaní:** Jedinci, kteří jsou nakažení a mohou šířit nemoc na náchylné jedince.
* **R (Recovered/Removed) – Uzdravení/Odstranění:** Jedinci, kteří se uzdravili, získali imunitu a již nemohou nemoc šířit, nebo zemřeli v důsledku nemoci.

Základní dynamika modelu spočívá v přechodech jedinců mezi těmito stavy:
* Náchylný jedinec se může stát infikovaným po kontaktu s infikovaným jedincem (s určitou pravděpodobností infekce).
* Infikovaný jedinec se po určité době (nebo s určitou pravděpodobností uzdravení) přesune do stavu uzdravený/odstraněný.

Cílem modelu je pochopit a predikovat dynamiku šíření epidemie, například jak rychle se bude nemoc šířit, kolik jedinců bude v maximu nakaženo a kdy epidemie pomine. V této práci je model simulován na diskrétní mřížce, kde každý uzel mřížky reprezentuje jedince a interakce probíhají mezi sousedními uzly.

## 2. Možnosti využití SIR modelu

SIR model a jeho varianty mají široké uplatnění:

* **Predikce průběhu epidemií:** Předpovídání počtu nakažených, hospitalizovaných a úmrtí pro nemoci jako chřipka, spalničky, COVID-19 atd.
* **Hodnocení dopadu intervencí:** Analýza účinnosti opatření jako jsou vakcinace, sociální distancování, karanténa nebo nošení roušek na zpomalení šíření nemoci.
* **Plánování zdrojů ve zdravotnictví:** Odhad potřebných kapacit lůžek, personálu a zdravotnického materiálu během epidemie.
* **Studium dynamiky nemocí:** Zkoumání, jak různé faktory (např. hustota populace, mobilita, struktura kontaktů) ovlivňují šíření infekce.
* **Výzkum v oblasti veřejného zdraví:** Poskytování podkladů pro rozhodování o strategiích prevence a kontroly nemocí.

## 3. Princip Algoritmu simulace SIR modelu

Simulace SIR modelu na mřížce probíhá v diskrétních krocích, kde každý jedinec (uzel) je ve stavu S (náchylný), I (infikovaný), nebo R (uzdravený). Na počátku je většina populace S, s několika jedinci I. V každém kroku se nejprve vyhodnotí potenciální změny: infikovaní jedinci mohou s danou pravděpodobností nakazit své náchylné sousedy a také se sami s danou pravděpodobností uzdravit. Tato vyhodnocení se provádějí na základě stavů z počátku kroku, aby se předešlo okamžitým kaskádovým efektům. Následně se stavy všech určených jedinců aktualizují (S na I, I na R). Sekvenční implementace (`SimpleSequentialGridSIRSolver`) nejprve shromáždí všechny kandidáty na změnu a poté jejich stavy hromadně aktualizuje. Paralelní přístupy, jako `ForkJoinGridSIRSolver`, rozdělují výpočetní práci mezi více vláken a musí zajistit konzistentní aktualizaci stavů (např. synchronizací), zejména u velkých mřížek. Simulace končí, když nejsou žádní infikovaní jedinci nebo je dosaženo maximálního počtu kroků, přičemž se po každém kroku sbírají statistiky.

### 4. Rozložení projektu (balíčky a klíčové třídy):

Projekt je strukturován do následujících hlavních balíčků:

* **`sir.analyzer`**: Obsahuje třídy zodpovědné za načítání konfigurací, zpracování výsledků simulací, jejich ukládání do CSV formátu a následnou vizualizaci pomocí generování grafů.
  * `Analyzer`: Třída, která zpracovává data vygenerovaná jednotlivými solvery. Pro každou "generaci" (jedno spuštění s danou konfigurací) a každý "run" (opakování v rámci generace) agreguje data a spojuje je do výsledného CSV souboru pro analýzu.
  * `ConfigLoader`: Načítá konfigurační soubory (`config.txt`) z příslušných složek, které obsahují parametry pro jednotlivé simulace.
  * `CsvWriter`: Po analýze a zpracování dat z jednotlivých runů a generací zapisuje agregované a zpracované statistiky do CSV souborů.
  * `GraphGenerator`: Generuje grafy z finálních CSV dat pro vizuální analýzu a porovnání výkonnosti různých solverů.
  * `RunStatsLoader`: Načítá statistiky jednotlivých běhů simulace.

* **`sir.grid`**: Zahrnuje třídy související se správou simulační mřížky a logováním.
  * `GridSupplier`: Zajišťuje vytvoření a poskytnutí simulační mřížky pro jednotlivé běhy.
  * `OutputManager`: Spravuje výstupní soubory a složky generované během analýzy a simulací.
  * `SimulationLogger`: Loguje průběh a klíčové události simulace pro účely ladění a sledování.

* **`sir.model`**: Definuje základní datové struktury, stavy a konfigurace pro SIR model.
  * `Configuration`: Uchovává parametry simulace jako pravděpodobnost infekce, pravděpodobnost uzdravení, rozměry mřížky, počet kroků simulace, typ solveru atd.
  * `Node` / `OptNode`: Reprezentuje jednotlivé uzly (jedince) v mřížce. `OptNode` je optimalizovaná verze `Node` pro lepší výkon.
  * `RunStats`: Agreguje statistiky za celý běh jedné simulace (např. celkový čas, počet tiků, průměrný čas kroku).
  * `State`: Enum definující stavy jedince (SUSCEPTIBLE, INFECTED, RECOVERED).
  * `StepResult`: Uchovává výsledek jednoho simulačního kroku; jedná se o obdobu `StepStats`, ale bez některých detailních statistik (např. tiků), což je přizpůsobeno implementaci `ForkJoinGridSIRSolver`.
  * `StepStats`: Obsahuje detailní statistiky pro jeden simulační krok (např. čas trvání kroku, počet zpracovaných tiků).

* **`sir.solver`**: Obsahuje různé implementace algoritmů pro běh simulace SIR modelu, včetně sekvenční a několika paralelních variant.
  * `SIRSolver`: Interface definující kontrakt, který musí splňovat všechny implementace solverů.
  * `SimpleSequentialGridSIRSolver`: Základní sekvenční implementace simulace SIR modelu na mřížce.
  * `ForkJoinGridSIRSolver`: Paralelní implementace využívající Fork/Join framework pro efektivní rozdělení práce na více vláken.
  * `CompletableFutureSIRSolver`: Paralelní implementace založená na `CompletableFuture` pro asynchronní zpracování simulačních kroků.
  * `SimpleParallelGridSIRSolver`: Jednoduchá, optimistická paralelní implementace.
  * `SimulationRunner`: Hlavní třída zodpovědná za nastavení parametrů simulace, sestavení jednotlivých `SimulationRunnerBuilder`ů pro různé konfigurace a následné spuštění simulací.
  * `SimulationRunnerBuilder`: Přijímá konfiguraci (objekt `Configuration`) a konkrétní implementaci `SIRSolver`. Na základě těchto vstupů vytváří a spouští jednotlivé simulační běhy.

### Jak s projektem pracovat:

1.  **Definice konfigurací a spuštění simulací:**
  * Konfigurace simulací (parametry jako velikost mřížky, pravděpodobnosti, typ solveru atd.) se definují přímo v kódu ve třídě `SimulationRunner`.
  * V `SimulationRunner` se následně sestaví instance `SimulationRunnerBuilder` pro každou požadovanou konfiguraci (kombinace parametrů a solveru).
  * Spuštěním `SimulationRunner` se iniciují jednotlivé simulační běhy.

2.  **Průběh simulace a ukládání prvotních dat:**
  * Během každého simulačního běhu v rámci dané generace jsou sbírány statistiky každého simulačního kroku ("ticku").
  * Po dokončení každého běhu se tyto detailní statistiky uloží do CSV souboru ve struktuře `output/gen{X}/{názevSolveru}/run{Y}.csv` (kde `{X}` je identifikátor generace/konfigurace a `{Y}` je číslo opakování).

3.  **Zpracování a agregace dat:**
  * Po dokončení všech simulací je nutné spustit třídu `Analyzer`.
  * `Analyzer` načte všechny vygenerované CSV soubory (`run{Y}.csv`) z jednotlivých generací a solverů.
  * Data jsou agregována (např. průměrováním přes opakované běhy v rámci jedné generace) a zpracována.
  * Výsledné agregované statistiky pro každý solver a každou generaci jsou uloženy do nových CSV souborů ve složce `output_processed/{názevSolveru}.csv`.

4.  **Generování grafů:**
  * Nakonec se spustí třída `GraphGenerator`.
  * `GraphGenerator` vezme zpracované CSV soubory ze složky `output_processed/` a vytvoří z nich grafy.
  * Tyto grafy vizualizují a porovnávají různé statistiky výkonnosti (např. průměrný čas kroku, celkový čas, rychlost) mezi jednotlivými implementacemi solverů a konfiguracemi. Grafy jsou uloženy do specifikované výstupní složky graphs

## 4. Zhodnocení výsledků a pozorování z grafů

V rámci experimentální části byly porovnávány různé implementace SIR solverů (sekvenční, Fork/Join, CompletableFuture, SimpleParallel) na různých velikostech mřížky. Pro vyhodnocení byly sledovány následující metriky, vizualizované v grafech:

* **`Avg Step Time`:**
  * Tento graf ukazuje, kolik času v průměru zabral jeden simulační krok.

* **`Total Time)`:**
  * Tento graf zobrazuje celkový čas potřebný k dokončení celé simulace pro danou konfiguraci.

* **`Avg Speed`:**
  * Vyšší hodnoty znamenají efektivnější zpracování simulačních tiků.
  
* **`Solver Comparison by Grid Size`:**
  * Tento srovnávací graf ukazuje, jak si jednotlivé solvery vedou při různých velikostech mřížky. Každá křivka reprezentuje jeden solver a znázorňuje jeho průměrný čas simulace pro různé konfigurace gridu. Umožňuje snadno porovnat škálovatelnost a výkon různých implementací.

**Obecná pozorování:**
* Implementace `ForkJoinGridSIRSolver` se ukazuje jako velmi rychlá a efektivní jak pro malé, tak i pro velké objemy dat. Její optimalizovaná implementace pro rozdělování práce jí dává výhodu i přes určitou režii spojenou s paralelismem, a je rychlejší než jednoduché sekvenční i jednoduché paralelní přístupy.
* `CompletableFutureSIRSolver` se jeví jako jedna z nejpomalejších implementací. Pro malé a střední velikosti mřížky je její výkon srovnatelný se sekvenčním řešením, avšak s narůstající velikostí dat (např. 2000x2000 a více) se její výkon dále zhoršuje a stává se výrazně pomalejší než ostatní paralelní přístupy a často i pomalejší než sekvenční implementace.
* `Simple_Parallel_Grid_SIR_Solver` je na menších datech méně výkonný než sekvenční implementace. Jak ale objem dat roste, stává se efektivnější a překonává sekvenční přístup.
* Sekvenční implementace `Simple_Sequential_Grid_SIR_Solver` je konkurenceschopná pro velmi malé mřížky, kde režie paralelismu u ostatních přístupů převyšuje přínosy plynoucí z paralelizace.
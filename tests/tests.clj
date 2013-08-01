;;;
;;; tests.clj -- Test the sort order for the browse handler.
;;;
;;; These tests were created to check that the browse handler was sorting
;;; headings as expected.  If you change anything relating to sorting, it's a
;;; good idea to run these tests (using the 'run.sh' script in this directory).
;;;
;;; More general tests for the browse functionality are better added to VuFind's
;;; integration test suite.

(ns tests
  (:use [clojure.java.io :as jio]
        [clojure.test :as test])
  (:import (org.apache.lucene.analysis.standard StandardAnalyzer)
           (org.apache.lucene.document Document Field Field$Store Field$Index)
           (org.apache.lucene.store FSDirectory)
           (org.apache.lucene.index IndexWriter IndexWriterConfig)
           (org.apache.lucene.util Version)
           (java.io File)
           (com.google.common.io Files)

           (org.apache.solr.core CoreContainer CoreContainer$Initializer)
           (org.apache.solr.client.solrj.embedded EmbeddedSolrServer)
           (org.apache.solr.client.solrj SolrQuery)))



(defn heading-document [field-name heading]
  (let [doc (Document.)]
    (.add doc (Field. field-name (:heading heading)
                      org.apache.lucene.document.Field$Store/YES
                      org.apache.lucene.document.Field$Index/NOT_ANALYZED))
    (when (:sort-key heading)
      (.add doc (Field. (str "sort-" field-name) (:sort-key heading)
                        org.apache.lucene.document.Field$Store/YES
                        org.apache.lucene.document.Field$Index/NOT_ANALYZED)))
    doc))


(defn populate-index [headings field-name index-file]
  (with-open [iw (IndexWriter.
                  (FSDirectory/open (jio/file index-file))
                  (doto (IndexWriterConfig.
                         Version/LUCENE_40
                         (StandardAnalyzer. Version/LUCENE_40))))]
    (doseq [heading headings]
      (.addDocument iw (heading-document field-name heading)))))


(defn do-browse [server browse-type & [opts]]
  (mapv #(.get % "heading")
        (-> (.query server
                    (doto (SolrQuery.)
                      (.setQueryType "/browse")
                      (.setParam "source" (into-array [browse-type]))
                      (.setParam "rows" (into-array ["100"]))
                      (.setParam "from" (into-array [(or (:from opts)
                                                         "")]))))
            .getResponse
            (.get "Browse")
            (.get "items"))))


(defn rm-rf [base]
  (doseq [file (filter #(.isFile %)
                       (file-seq (file base)))]
    (.delete file))
  (doseq [dir (clojure.core/reverse (file-seq (file base)))]
    (.delete dir)))


(def test-browses [{:name "author"
                    :headings  [{:heading "AAA"}
                                {:heading "Äardvark"}
                                {:heading "Apple"}
                                {:heading "Orange"}
                                {:heading "Banana"}
                                {:heading "grapefruit"}]}

                   {:name "title"
                    :headings  [{:heading "AAA"
                                 :sort-key "AAA"}
                                {:heading "Äardvark"
                                 :sort-key "Äardvark"}
                                {:heading "Apple"
                                 :sort-key "Apple"}
                                {:heading "Orange"
                                 :sort-key "Orange"}
                                {:heading "Banana"
                                 :sort-key "Banana"}
                                {:heading "grapefruit"
                                 :sort-key "grapefruit"}
                                {:heading "\"Hyphenated-words and double quotes\""
                                 :sort-key "\"hyphenated-words and double quotes\""}
                                {:heading "   inappropriate leading space"
                                 :sort-key "   inappropriate leading space"}]}

                   {:name "sample0", :headings [{:heading "Adoración nocturna española : sección de San Sebastián : datos históricos de su fundación, desarrollo y actuación, bodas de plata y memoria de sus bodas de oro : 1905-1955", :sort-key "adoración nocturna española: sección de san sebastián : datos históricos de su fundación, desarrollo y actuación, bodas de plata y memoria de sus bodas de oro : 1905-1955"} {:heading "Ahatetxoa eta sahats negartia", :sort-key "ahatetxoa eta sahats negartia"} {:heading "Análisis de las aguas sulfurosas frías ferro-manganíferas nitrogenadas de los manatiales y de los baños y castañar de Ormáiztegui (Guipúzcoa)", :sort-key "análisis de las aguas sulfurosas frías ferro-manganíferas nitrogenadas de los manatiales y de los baños y castañar de ormáiztegui (guipúzcoa)"} {:heading "Atlas geográfico, histórico y estadístico de España y sus posesiones de Ultramar", :sort-key "atlas geográfico, histórico y estadístico de españa y sus posesiones de ultramar"} {:heading "Una aventura olímpica", :sort-key "aventura olímpica"} {:heading "Belokeko abatetxearen historia : ehun urte jainkosemen alde : (1875-1975)", :sort-key "belokeko abatetxearen historia: ehun urte jainkosemen alde : (1875-1975)"} {:heading "Christmas Carol Festival [Música impresa]", :sort-key "christmas carol festival"} {:heading "Crazy Heart [Grabación sonora] : Original Motion Picture Soundtrack", :sort-key "crazy heart: original motion picture soundtrack"} {:heading "Cuentos para niños", :sort-key "cuentos para niños"} {:heading "Cuerpo de oficiales de la administración de justicia : temario teórico : volumen 1.", :sort-key "cuerpo de oficiales de la administración de justicia: temario teórico : volumen 1."} {:heading "Desastres naturales [Vídeo]", :sort-key "desastres naturales"} {:heading "Errepideko mamua", :sort-key "errepideko mamua"} {:heading "Etude op. 11 no. 4 [Música impresa] : marimba", :sort-key "etude op. 11 no. 4: marimba"} {:heading "Fines de la pena : (importancia, dificultad y actualidad del tema)", :sort-key "fines de la pena: (importancia, dificultad y actualidad del tema)"} {:heading "Los hijos de la tierra 2. El valle de los caballos 1.", :sort-key "hijos de la tierra"} {:heading "Introducción a la didáctica de la lengua y la literatura : un enfoque sociocrítico", :sort-key "introducción a la didáctica de la lengua y la literatura: un enfoque sociocrítico"} {:heading "Jim Jam & Sunny. ¡Qué bueno es compartir!. Soñar despiertos [DVD-Vídeo]", :sort-key "jim jam & sunny"} {:heading "Jonasek arazo potolo bat du", :sort-key "jonasek arazo potolo bat du"} {:heading "Koldo Mitxelena entre nosotros", :sort-key "koldo mitxelena entre nosotros"} {:heading "El Laberinto sentimental", :sort-key "laberinto sentimental"}]}

                   {:name "sample1", :headings [{:heading "101 dálmatas [DVD-Vídeo] : más vivos que nunca", :sort-key "101 dálmatas: más vivos que nunca"} {:heading "Amorante eskuzabala", :sort-key "amorante eskuzabala"} {:heading "Antología de la Nueva Trova Cubana [Grabación sonora] : 25 aniversario", :sort-key "antología de la nueva trova cubana: 25 aniversario"} {:heading "El árbol de los cuentos : cuentos reunidos 1973-2004", :sort-key "árbol de los cuentos: cuentos reunidos 1973-2004"} {:heading "La Batalla de Tebas : Egipto contra los hicsos", :sort-key "batalla de tebas: egipto contra los hicsos"} {:heading "Bob Marley : \"Positive vibration\"", :sort-key "bob marley: \"positive vibration\""} {:heading "La Clave", :sort-key "clave"} {:heading "El clavo [DVD-Vídeo]", :sort-key "clavo"} {:heading "Climate change : impacts and responses", :sort-key "climate change : impacts and responses"} {:heading "Il Colore nel cinema", :sort-key "colore nel cinema"} {:heading "Comentario al fuero de los españoles : teoria jurídica de los derechos humanos. IV", :sort-key "comentario al fuero de los españoles: teoria jurídica de los derechos humanos. iv"} {:heading "Cómo hablar en público y no parecer un idiota", :sort-key "cómo hablar en público y no parecer un idiota"} {:heading "Con el consentimiento del cuerpo", :sort-key "con el consentimiento del cuerpo"} {:heading "Cosas del lenguaje : etimología, lexicología, semántica", :sort-key "cosas del lenguaje: etimología, lexicología, semántica"} {:heading "El documento fotográfico :Historia, usos y aplicaciones", :sort-key "documento fotográfico:historia, usos y aplicaciones"} {:heading "Doña Piñones", :sort-key "doña piñones"} {:heading "Drum Sessions [Música impresa] : Comprehensive Method for Individual or Group Study : Book 2", :sort-key "drum sessions: comprehensive method for individual or group study : book 2"} {:heading "Grafología", :sort-key "grafología"} {:heading "Guerra en la familia", :sort-key "guerra en la familia"} {:heading "Der Hundetraum : und andere Verwirrungen", :sort-key "hundetraum: und andere verwirrungen"}]}

                   {:name "sample2", :headings [{:heading "55 Rayuelas", :sort-key "55 rayuelas"} {:heading "La administración local : primera parte memoria sobre los vicios y abusos existentes en los municipios, segunda parte proyectos y bases para corregirlos", :sort-key "administración local: primera parte memoria sobre los vicios y abusos existentes en los municipios, segunda parte proyectos y bases para corregirlos"} {:heading "Alrededor del trabajo de la madera : un estudio completo del trabajo de la madera mediante máquinas", :sort-key "alrededor del trabajo de la madera: un estudio completo del trabajo de la madera mediante máquinas"} {:heading "Arrazoimen praktikoaren kritika", :sort-key "arrazoimen praktikoaren kritika"} {:heading "El Barroco : arquitectura, escultura, pintura", :sort-key "barroco: arquitectura, escultura, pintura"} {:heading "Calígrafos vascos : (nómina)", :sort-key "calígrafos vascos: (nómina)"} {:heading "Colección de fotografía contemporánea de Telefónica :[exposición]", :sort-key "colección de fotografía contemporánea de telefónica:[exposición]"} {:heading "Comedias escogidas de Fray Lope Felix de Vega Carpio", :sort-key "comedias escogidas de fray lope felix de vega carpio"} {:heading "La cuestión del Puerto de la Paz y la Zamacolada : exposición historica acompañada de la memoria justificativa de uno de los actores de aquellos sucesos", :sort-key "cuestión del puerto de la paz y la zamacolada: exposición historica acompañada de la memoria justificativa de uno de los actores de aquellos sucesos"} {:heading "Derecho Urbanístico del País Vasco", :sort-key "derecho urbanístico del país vasco"} {:heading "Estructura y Sonoridad de los Instrumentos de Arco : el arco y el violín pieza por pieza : cuesgtionario para la enseñanza y exámenes", :sort-key "estructura y sonoridad de los instrumentos de arco: el arco y el violín pieza por pieza : cuesgtionario para la enseñanza y exámenes"} {:heading "Identidad y convivencia en el País Vasco : bajo los volcanes", :sort-key "identidad y convivencia en el país vasco: bajo los volcanes"} {:heading "Las islas de la felicidad", :sort-key "islas de la felicidad"} {:heading "Manitas y el señor Ningunproblema. Contra la Banda de la Brocha [Recurso electrónico]", :sort-key "manitas y el señor ningunproblema"} {:heading "Motin en la Bounty", :sort-key "motin en la bounty"} {:heading "Los niños de la guerra", :sort-key "niños de la guerra"} {:heading "No sin mi hija [DVD-Vídeo] = Not without my daughter", :sort-key "no sin mi hija= not without my daughter"} {:heading "Objetivo: la luna", :sort-key "objetivo: la luna"} {:heading "Odes [Grabación sonora]", :sort-key "odes"} {:heading "Peru Abarka", :sort-key "peru abarka"}]}

                   {:name "sample3", :headings [{:heading "Aguas primaverales ; Fiesta ; Adiós a las armas ; Tener y no tener", :sort-key "aguas primaverales; fiesta ; adiós a las armas ; tener y no tener"} {:heading "La autonomía fiscal en Euskal Herria : una aproximación a los impuestos sobre rendimientos de las personas físicas y sobre sociedades", :sort-key "autonomía fiscal en euskal herria: una aproximación a los impuestos sobre rendimientos de las personas físicas y sobre sociedades"} {:heading "Un caso para ti y el equipo tigre 1. En el templo de los truenos", :sort-key "caso para ti y el equipo tigre"} {:heading "Complete music with orchestra [Grabación sonora] 3", :sort-key "complete music with orchestra"} {:heading "La conversión del capitán Brassbound : comedia en tres actos y en prosa", :sort-key "conversión del capitán brassbound: comedia en tres actos y en prosa"} {:heading "Una Cruzada en solitario", :sort-key "cruzada en solitario"} {:heading "Érase una vez__la música. Piotr Ilich Chaikovski [Grabación sonora]", :sort-key "érase una vez__la música"} {:heading "Eric Clapton", :sort-key "eric clapton"} {:heading "Feral y las cigüeñas", :sort-key "feral y las cigüeñas"} {:heading "El Fuego Originario [DVD-Vídeo] : El Baile", :sort-key "fuego originario: el baile"} {:heading "La guía del inversor en arte : una referencia imprescindible para todos los coleccionistas de arte : la obra gráfica moderna y contemporánea como modelo", :sort-key "guía del inversor en arte: una referencia imprescindible para todos los coleccionistas de arte : la obra gráfica moderna y contemporánea como modelo"} {:heading "Inteligencia erótica : claves para mantener la pasión en la pareja", :sort-key "inteligencia erótica: claves para mantener la pasión en la pareja"} {:heading "Jazz standards for drumset [Música impresa] : a comprehensive guide to authentic jazz playing using 12 must-know tunes", :sort-key "jazz standards for drumset: a comprehensive guide to authentic jazz playing using 12 must-know tunes"} {:heading "Music for the Spanish kings [Grabación sonora]", :sort-key "music for the spanish kings"} {:heading "Nirvana [Música impresa] : drum collection", :sort-key "nirvana: drum collection"} {:heading "Okupada", :sort-key "okupada"} {:heading "Patrón de embarcaciones de recreo", :sort-key "patrón de embarcaciones de recreo"} {:heading "Persiar gutunak", :sort-key "persiar gutunak"} {:heading "Secuestrado", :sort-key "secuestrado"}]}

                   {:name "sample4", :headings [{:heading "Biblioteca para pacientes de hospital : Libros para adultos : catálogo", :sort-key "biblioteca para pacientes de hospital: libros para adultos : catálogo"} {:heading "Bidetik : semblanza de Joxe Mari Korta", :sort-key "bidetik: semblanza de joxe mari korta"} {:heading "La crisis del humanismo : los principios de autoridad, libertad y función a la luz de la guerra : (una crítica de la autoridad y de la libertad como fundamentos del Estado moderno...)", :sort-key "crisis del humanismo: los principios de autoridad, libertad y función a la luz de la guerra : (una crítica de la autoridad y de la libertad como fundamentos del estado moderno...)"} {:heading "Cuentos de la Condesa de Segur", :sort-key "cuentos de la condesa de segur"} {:heading "De viajes : revista práctica .", :sort-key "de viajes: revista práctica ."} {:heading "Du contrat de société civile et commerciale ou Commentaire du titre IX du livre III du Code Civil", :sort-key "du contrat de société civile et commerciale ou commentaire du titre ix du livre iii du code civil"} {:heading "Enciclopedia de jardinería : ideas para cultivar practicamente de todo", :sort-key "enciclopedia de jardinería: ideas para cultivar practicamente de todo"} {:heading "Expresión corporal : ejercicios y sugerencias", :sort-key "expresión corporal: ejercicios y sugerencias"} {:heading "Las Fieras fútbol club 8. Fabi el gran extremo derecho", :sort-key "fieras fútbol club"} {:heading "Fragile [Grabación sonora]", :sort-key "fragile"} {:heading "El frío y las tinieblas : el mundo después de una guerra nuclear", :sort-key "frío y las tinieblas: el mundo después de una guerra nuclear"} {:heading "Guinness world records DVD 1 [DVD-Vídeo]", :sort-key "guinness world records"} {:heading "Honeysuckle [Grabación sonora] : (mesk elil)", :sort-key "honeysuckle: (mesk elil)"} {:heading "La huella de Lorca", :sort-key "huella de lorca"} {:heading "Ipui onak", :sort-key "ipui onak"} {:heading "Izurri berria", :sort-key "izurri berria"} {:heading "Machines marines : cours de machines a vapeur, professé a l'école d'application du génie maritime", :sort-key "machines marines: cours de machines a vapeur, professé a l'école d'application du génie maritime"} {:heading "New York blues", :sort-key "new york blues"} {:heading "La parole perdue", :sort-key "parole perdue"} {:heading "Partitura para saxo", :sort-key "partitura para saxo"}]}

                   {:name "sample5", :headings [{:heading "Accordion Jolly [Música impresa] : fox novelty", :sort-key "accordion jolly: fox novelty"} {:heading "Alimentos para tu salud", :sort-key "alimentos para tu salud"} {:heading "Astelehenean baserrian, asteartean-- : Lezoko baserriak lehen eta orain", :sort-key "astelehenean baserrian, asteartean--: lezoko baserriak lehen eta orain"} {:heading "Desarrollo sostenible : un concepto polémico", :sort-key "desarrollo sostenible: un concepto polémico"} {:heading "Developing a jazz language", :sort-key "developing a jazz language"} {:heading "Dibujos de arquitectura y ornamentación del siglo XVIII : Biblioteca Nacional de España, Madrid, 17 de septiembre a 22 de noviembre de 2009", :sort-key "dibujos de arquitectura y ornamentación del siglo xviii: biblioteca nacional de españa, madrid, 17 de septiembre a 22 de noviembre de 2009"} {:heading "Enigmas del Universo", :sort-key "enigmas del universo"} {:heading "Guía o estado general de la Real Hacienda de España : año 1818", :sort-key "guía o estado general de la real hacienda de españa: año 1818"} {:heading "Hamar Urte Euskal kulturan: 1983-1993", :sort-key "hamar urte euskal kulturan: 1983-1993"} {:heading "Hearts and bones [Grabación sonora]", :sort-key "hearts and bones"} {:heading "Jose María Merino : el retrato de lo cotidiano contra la \"muerte de", :sort-key "jose maría merino: el retrato de lo cotidiano contra la \"muerte de"} {:heading "Kai, el de la caja", :sort-key "kai, el de la caja"} {:heading "Koioteren arrastoa : USAko kontakizunak", :sort-key "koioteren arrastoa: usako kontakizunak"} {:heading "Mediterráneo [Grabación sonora]", :sort-key "mediterráneo"} {:heading "La móprea", :sort-key "móprea"} {:heading "Las Mujeres en la Comunidad Autónoma de Euskadi", :sort-key "mujeres en la comunidad autónoma de euskadi"} {:heading "El Palomo cojo", :sort-key "palomo cojo"} {:heading "Los papeles póstumos del Club Pickwick", :sort-key "papeles póstumos del club pickwick"} {:heading "Traidor, inconfeso y mártir", :sort-key "traidor, inconfeso y mártir"} {:heading "Los vascos en la Argentina : familias y protagonismo", :sort-key "vascos en la argentina: familias y protagonismo"}]}

                   {:name "sample6", :headings [{:heading "Algunos hombres buenos", :sort-key "algunos hombres buenos"} {:heading "Antología del pop español [Grabación sonora]", :sort-key "antología del pop español"} {:heading "Aprende matemáticas con Dikie & Dukie [Recurso electrónico]", :sort-key "aprende matemáticas con dikie & dukie"} {:heading "Blue spirits [Grabación sonora]", :sort-key "blue spirits"} {:heading "Catálogo tipológico del cuento folklórico español : cuentos de animales", :sort-key "catálogo tipológico del cuento folklórico español: cuentos de animales"} {:heading "Comprender la ansiedad, las fobias y el estrés", :sort-key "comprender la ansiedad, las fobias y el estrés"} {:heading "Córcega", :sort-key "córcega"} {:heading "Doraemon, el gladiador [DVD-Vídeo]", :sort-key "doraemon, el gladiador"} {:heading "Eluana : la libertad y la vida", :sort-key "eluana: la libertad y la vida"} {:heading "En el amor y en la guerra", :sort-key "en el amor y en la guerra"} {:heading "Googled = (Googleados) : el fin del mundo tal como lo conocíamos", :sort-key "googled= (googleados) : el fin del mundo tal como lo conocíamos"} {:heading "Guía de libros recomendados 1992", :sort-key "guía de libros recomendados 1992"} {:heading "?Han matado a Kronopius!", :sort-key "han matado a kronopius!"} {:heading "Historia de España Labor 2. Romanismo y Germanismo, el despertar de los pueblos hispánicos (S.IV-X)", :sort-key "historia de españa labor"} {:heading "Historia y guía de los museos de España", :sort-key "historia y guía de los museos de españa"} {:heading "Lady Macbetch of mtsensk [DVD-Vídeo]", :sort-key "lady macbetch of mtsensk"} {:heading "Long John Silver 3 El laberinto de esmeralda", :sort-key "long john silver"} {:heading "Un matrimonio muy... muy... muy feliz : obra en dos actos", :sort-key "matrimonio muy... muy... muy feliz: obra en dos actos"} {:heading "La muerte de dios : la cultura de nuestra era postcristiana", :sort-key "muerte de dios: la cultura de nuestra era postcristiana"} {:heading "Oeuvres complétes de P.J. de Beranger", :sort-key "oeuvres complétes de p.j. de beranger"}]}

                   {:name "sample7", :headings [{:heading "Animar a la lectura", :sort-key "animar a la lectura"} {:heading "Atlas ilustrado de los fósiles", :sort-key "atlas ilustrado de los fósiles"} {:heading "The bell jar", :sort-key "bell jar"} {:heading "De parte de la princesa muerta", :sort-key "de parte de la princesa muerta"} {:heading "Las energías renovables y medio ambiente", :sort-key "energías renovables y medio ambiente"} {:heading "Enigmas misteriosos, respuestas sorprendentes", :sort-key "enigmas misteriosos, respuestas sorprendentes"} {:heading "[Dibujos originales para la edición de Marina] [Material gráfico]", :sort-key "ibujos originales para la edición de marina"} {:heading "Jazzy [Música impresa] : for young players : flute 1", :sort-key "jazzy: for young players : flute 1"} {:heading "Leonor de Castilla", :sort-key "leonor de castilla"} {:heading "El Libro de la selva 2 [DVD-Vídeo]", :sort-key "libro de la selva"} {:heading "Matilde y la enredadera", :sort-key "matilde y la enredadera"} {:heading "Neu naiz indartsuena", :sort-key "neu naiz indartsuena"} {:heading "La Pena de muerte y los derechos humanos", :sort-key "pena de muerte y los derechos humanos"} {:heading "¡Qué gran bailarina!", :sort-key "qué gran bailarina!"} {:heading "Qué pequeño es el mundo", :sort-key "qué pequeño es el mundo"} {:heading "Ramsés 1 : el hijo de la luz", :sort-key "ramsés 1: el hijo de la luz"} {:heading "Sicilia", :sort-key "sicilia"} {:heading "Thailandia e Indochina", :sort-key "thailandia e indochina"} {:heading "Vida, naturaleza y ciencia : todo lo que hay que saber", :sort-key "vida, naturaleza y ciencia: todo lo que hay que saber"}]}

                   {:name "sample8", :headings [{:heading "Abrége de géographie moderne", :sort-key "abrége de géographie moderne"} {:heading "Bibliotheca Hispana nova, sive Hispanorum scriptorum", :sort-key "bibliotheca hispana nova, sive hispanorum scriptorum"} {:heading "Burdindenda", :sort-key "burdindenda"} {:heading "Los Conciertos económicos", :sort-key "conciertos económicos"} {:heading "Daniel Cortis", :sort-key "daniel cortis"} {:heading "Django Reinhardt and his american friends [Grabación sonora] : complete sessions", :sort-key "django reinhardt and his american friends: complete sessions"} {:heading "En los polos", :sort-key "en los polos"} {:heading "Essential Grammar in use : a self-study reference and practice book for elementary students of English: with answers", :sort-key "essential grammar in use: a self-study reference and practice book for elementary students of english: with answers"} {:heading "El ferrocarril directo de Valladolid á Vigo y las comunicaciones ferroviarios con Galicia", :sort-key "ferrocarril directo de valladolid á vigo y las comunicaciones ferroviarios con galicia"} {:heading "Filosofía de la ciencia", :sort-key "filosofía de la ciencia"} {:heading "Global Entrepreneurship Monitor : Comunidad Autónoma del País Vasco : informe ejecutivo", :sort-key "global entrepreneurship monitor: comunidad autónoma del país vasco : informe ejecutivo"} {:heading "Gran atlas de España Planeta", :sort-key "gran atlas de españa planeta"} {:heading "Los Hermanos Xabier y Martín Etxeberria ganadores del Lizardi", :sort-key "hermanos xabier y martín etxeberria ganadores del lizardi"} {:heading "Historia de las Islas del archipiélago filipino y reinos de la gran China, Tartaria, Cochinchina, Malaca, Siam, Cambodge y Japón", :sort-key "historia de las islas del archipiélago filipino y reinos de la gran china, tartaria, cochinchina, malaca, siam, cambodge y japón"} {:heading "Isère : patrimoines et musées : atlas interactif : musée de l'ancien évêché, patrimoines de l'isère, baptistère de Grenoble [Recurso electrónico]", :sort-key "isère: patrimoines et musées : atlas interactif : musée de l'ancien évêché, patrimoines de l'isère, baptistère de grenoble"} {:heading "¡Liberad a Willy! [DVD-Vídeo]", :sort-key "liberad a willy!"} {:heading "Mahoma : la novela de un profeta", :sort-key "mahoma: la novela de un profeta"} {:heading "Los mejores cortos de Pixar Volumen 1 [DVD-Vídeo]", :sort-key "mejores cortos de pixar"} {:heading "El milagro de la vida", :sort-key "milagro de la vida"} {:heading "La nueva publicidad : las mejores campañas en la era de internet", :sort-key "nueva publicidad: las mejores campañas en la era de internet"}]}

                   {:name "sample9", :headings [{:heading "Un Caimán en Nueva York", :sort-key "caimán en nueva york"} {:heading "Conjeturas sobre vasijas", :sort-key "conjeturas sobre vasijas"} {:heading "La familia y el cambio político en España", :sort-key "familia y el cambio político en españa"} {:heading "Green man [Grabación sonora]", :sort-key "green man"} {:heading "Guide du Forez : syndicat âdinitiative", :sort-key "guide du forez: syndicat âdinitiative"} {:heading "La Habitación secreta", :sort-key "habitación secreta"} {:heading "Instrucción reglamentaria para el conocimiento, manejo, conservación...conocido con el nombre de Modelo 1867 : señalada de texto para todos los Institutos", :sort-key "instrucción reglamentaria para el conocimiento, manejo, conservación...conocido con el nombre de modelo 1867: señalada de texto para todos los institutos"} {:heading "Jane juega y gana", :sort-key "jane juega y gana"} {:heading "El libro del comercio electrónico", :sort-key "libro del comercio electrónico"} {:heading "Mujeres en el sistema del arte en España", :sort-key "mujeres en el sistema del arte en españa"} {:heading "La paz perpetua [Recurso electrónico]", :sort-key "paz perpetua"} {:heading "Sakelako poemak", :sort-key "sakelako poemak"} {:heading "Santiago de Chile (1541-1991) : historia de una sociedad urbana", :sort-key "santiago de chile (1541-1991): historia de una sociedad urbana"} {:heading "Tomás Hernández Mendizabal : exposición de carteles (pinturas originales): Sala de Cultura de la Caja de Ahorros Municipal, 8 al 31 de enero de 1983", :sort-key "tomás hernández mendizabal: exposición de carteles (pinturas originales): sala de cultura de la caja de ahorros municipal, 8 al 31 de enero de 1983"} {:heading "XVI Quincena Fotográfica Vizcaina", :sort-key "xvi quincena fotográfica vizcaina"} {:heading "Yoga durante el embarazo [DVD-Vídeo] + Yoga postparto", :sort-key "yoga durante el embarazo+ yoga postparto"} {:heading "Zazpikotea sanferminetan", :sort-key "zazpikotea sanferminetan"}]}

                   {:name "sample10", :headings [{:heading "The Black Madonna and other stories", :sort-key "black madonna and other stories"} {:heading "Botila batean aurkitutako eskuizkribua eta beste ipuin batzuk", :sort-key "botila batean aurkitutako eskuizkribua eta beste ipuin batzuk"} {:heading "Un Corazón de nadie : antología poética (1913-1935)", :sort-key "corazón de nadie: antología poética (1913-1935)"} {:heading "Ekaitzpean : ipuinberri", :sort-key "ekaitzpean: ipuinberri"} {:heading "Elementa juris civilis ad usum studiosae juventutis", :sort-key "elementa juris civilis ad usum studiosae juventutis"} {:heading "Guía de plantación : contexto, objetivos, estructura, color, estaciones, estilos, condiciones", :sort-key "guía de plantación: contexto, objetivos, estructura, color, estaciones, estilos, condiciones"} {:heading "Hara : centro vital del hombre", :sort-key "hara: centro vital del hombre"} {:heading "Hay luz en el desván", :sort-key "hay luz en el desván"} {:heading "Hello Hoobs 8. Sports and music : let's play sports! ; our music band", :sort-key "hello hoobs"} {:heading "Historia de la música pop : vol. 3 : hasta 1963 - 1970 : la época dorada", :sort-key "historia de la música pop: vol. 3 : hasta 1963 - 1970 : la época dorada"} {:heading "Kamikaze Kaito Jeanne 2", :sort-key "kamikaze kaito jeanne"} {:heading "Kurdistani [Grabación sonora]", :sort-key "kurdistani"} {:heading "Manual de cocina económica vasca", :sort-key "manual de cocina económica vasca"} {:heading "Miguel Zapata [Folleto]", :sort-key "miguel zapata"} {:heading "Museos de San Sebastián : 2009 día internacional de los Museos = Donostiako museoak : 2009 museoaren Nazioarteko eguna [Folleto]", :sort-key "museos de san sebastián: 2009 día internacional de los museos = donostiako museoak : 2009 museoaren nazioarteko eguna"} {:heading "Naruto 17 : ¡El poder de Itachi!", :sort-key "naruto: ¡el poder de itachi!"} {:heading "Ricardo III", :sort-key "ricardo iii"} {:heading "Risas peligrosas", :sort-key "risas peligrosas"} {:heading "Scaramouche", :sort-key "scaramouche"} {:heading "Sing along with Acid House Kings [Grabación sonora]", :sort-key "sing along with acid house kings"}]}

                   {:name "sample11", :headings [{:heading "10 ans avec la flûte", :sort-key "10 ans avec la flûte"} {:heading "1984-2008, 25 urtez pobretasuna ikertzen Euskadin : Eusko Jaurlaritzaren Justizia, Lan eta Gizarte Segurantza sailak 1984 eta 2008 bitartean egindako azterketa eta lan estatistikoen laburpena", :sort-key "1984-2008, 25 urtez pobretasuna ikertzen euskadin: eusko jaurlaritzaren justizia, lan eta gizarte segurantza sailak 1984 eta 2008 bitartean egindako azterketa eta lan estatistikoen laburpena"} {:heading "Una Apuesta : comedia en un acto", :sort-key "apuesta: comedia en un acto"} {:heading "Una canción para el verano", :sort-key "canción para el verano"} {:heading "La cátedra de la calavera", :sort-key "cátedra de la calavera"} {:heading "Charro Bizarro", :sort-key "charro bizarro"} {:heading "El constructor de pirámides", :sort-key "constructor de pirámides"} {:heading "Cubridle el rostro", :sort-key "cubridle el rostro"} {:heading "Daddy", :sort-key "daddy"} {:heading "Discovering chi [DVD-Vídeo] = En busca del chi", :sort-key "discovering chi= en busca del chi"} {:heading "Los estilos del arte", :sort-key "estilos del arte"} {:heading "Goethe", :sort-key "goethe"} {:heading "Los gritos del silencio [DVD-Video]", :sort-key "gritos del silencio"} {:heading "La interpretación del asesinato", :sort-key "interpretación del asesinato"} {:heading "Italiano 1 . Base [Recurso electrónico]", :sort-key "italiano 1 . base"} {:heading "Juego nocturno", :sort-key "juego nocturno"} {:heading "Juventud y exclusión social : décimo foro sobre tendencias sociales", :sort-key "juventud y exclusión social: décimo foro sobre tendencias sociales"} {:heading "Kantatzera noazu-- [Grabación sonora]", :sort-key "kantatzera noazu--"} {:heading "La mosca [DVD-Vídeo]", :sort-key "mosca"} {:heading "Los \"Otros\" : la deportación de los \"sin papeles\" en Europa", :sort-key "otros\": la deportación de los \"sin papeles\" en europa"}]}

                   {:name "sample12", :headings [{:heading "Aromas del pasado", :sort-key "aromas del pasado"} {:heading "Camila", :sort-key "camila"} {:heading "Cuadros", :sort-key "cuadros"} {:heading "Dispersión y destrucción del Patrimonio Artístico Español : Volumen IV . [Desde comienzos de siglo hasta la Guerra Civil] 1900-1936", :sort-key "dispersión y destrucción del patrimonio artístico español"} {:heading "Emigrantes y refugiados : el derecho universal de la ciudadanía", :sort-key "emigrantes y refugiados: el derecho universal de la ciudadanía"} {:heading "Flipped [DVD-Vídeo]", :sort-key "flipped"} {:heading "La Fuga de Bach : juguete cómico, en tres actos y en prosa original", :sort-key "fuga de bach: juguete cómico, en tres actos y en prosa original"} {:heading "Guía de Kenia y Tanzania", :sort-key "guía de kenia y tanzania"} {:heading "La hemeroteca, parte esencial de la biblioteca pública", :sort-key "hemeroteca, parte esencial de la biblioteca pública"} {:heading "Leitza larrea [Grabación sonora]", :sort-key "leitza larrea"} {:heading "Ordenanzas de la ilustre universidad, casa de contratacion, y consulado de la M. Noble, y M. Leal ciudad de San Sebastian", :sort-key "ordenanzas de la ilustre universidad, casa de contratacion, y consulado de la m. noble, y m. leal ciudad de san sebastian"} {:heading "Un País en la mochila [DVD-Vídeo] : el Priorato", :sort-key "país en la mochila: el priorato"} {:heading "Patagonia : el último confín de la naturaleza = Nature's Last Frontier", :sort-key "patagonia: el último confín de la naturaleza = nature's last frontier"} {:heading "Tiempo de silencio", :sort-key "tiempo de silencio"} {:heading "Tierra fría", :sort-key "tierra fría"} {:heading "Vientos de guerra", :sort-key "vientos de guerra"} {:heading "Yo leo : una experiencia de biblioteca de aula en el ciclo medio", :sort-key "yo leo: una experiencia de biblioteca de aula en el ciclo medio"}]}

                   {:name "sample13", :headings [{:heading "El Angel descuidado", :sort-key "angel descuidado"} {:heading "Atlas de la exploración espacial", :sort-key "atlas de la exploración espacial"} {:heading "Bibliotecas en una sociedad desescolarizada", :sort-key "bibliotecas en una sociedad desescolarizada"} {:heading "Boccamurata", :sort-key "boccamurata"} {:heading "As cantigas galego-portuguesas", :sort-key "cantigas galego-portuguesas"} {:heading "Cristales", :sort-key "cristales"} {:heading "\"Cuando los árboles bailan\"", :sort-key "cuando los árboles bailan\""} {:heading "Cuarto Congreso Internacional de Educación popular : sección 1a enseñanza técnica", :sort-key "cuarto congreso internacional de educación popular: sección 1a enseñanza técnica"} {:heading "Diario de Bolivia", :sort-key "diario de bolivia"} {:heading "Es cuento largo", :sort-key "es cuento largo"} {:heading "La forma en que algunos mueren", :sort-key "forma en que algunos mueren"} {:heading "Frankenstein : el cómic", :sort-key "frankenstein: el cómic"} {:heading "La gran enciclopedia de los anfibios y reptiles", :sort-key "gran enciclopedia de los anfibios y reptiles"} {:heading "Guía para educar con disciplina y cariño : para que sus hijos sean amables, comprensivos y respetuosos", :sort-key "guía para educar con disciplina y cariño: para que sus hijos sean amables, comprensivos y respetuosos"} {:heading "Hasta el verano que viene", :sort-key "hasta el verano que viene"} {:heading "Hellboy : semilla de destrucción", :sort-key "hellboy: semilla de destrucción"} {:heading "Ice haven", :sort-key "ice haven"} {:heading "Le jardinier des salons ou L'art de cultiver les pleurs : dans les appartements, sur les croisées et sur les balcons : orné de jolies gravures", :sort-key "jardinier des salons ou l'art de cultiver les pleurs: dans les appartements, sur les croisées et sur les balcons : orné de jolies gravures"} {:heading "José S. Carralero : Retrospectiva 1959-1995 [catálogo exposición. León, Sala Provincia, enero-febrero 1996]", :sort-key "josé s. carralero: retrospectiva 1959-1995 [catálogo exposición. león, sala provincia, enero-febrero 1996]"} {:heading "La música en la Real Sociedad Bascongada de los Amigos del País : actividades desarrolladas en San Sebastián durante el periodo comprendido entre finales del siglo XIX y principios del XX, 1892-1912", :sort-key "música en la real sociedad bascongada de los amigos del país: actividades desarrolladas en san sebastián durante el periodo comprendido entre finales del siglo xix y principios del xx, 1892-1912"}]}

                   {:name "sample14", :headings [{:heading "Alaska", :sort-key "alaska"} {:heading "Bizikidetza eta gatazkak ikastetxeetan : EAEko Bigarren Hezkuntzako ikastetxeetan Arartekoak egindako ikerketa = Convivencia y conflictos en los centros educativos: Informe del Ararteko sobre la situación en los centros de Educación Secundaria de la CAPV", :sort-key "bizikidetza eta gatazkak ikastetxeetan: eaeko bigarren hezkuntzako ikastetxeetan arartekoak egindako ikerketa = convivencia y conflictos en los centros educativos: informe del ararteko sobre la situación en los centros de educación secundaria de la capv"} {:heading "El Camarón de la Isla [Grabación sonora] : calle Real (1983)", :sort-key "camarón de la isla: calle real (1983)"} {:heading "Los Castillos medievales", :sort-key "castillos medievales"} {:heading "Diario de Estela : ¡a por mis alas!", :sort-key "diario de estela: ¡a por mis alas!"} {:heading "Discursos leídos ante la Real Academia de Bellas Artes de San Fernando en la recepción pública del señor D.José Ramón Mélida el día 25 de marzo de 1899", :sort-key "discursos leídos ante la real academia de bellas artes de san fernando en la recepción pública del señor d.josé ramón mélida el día 25 de marzo de 1899"} {:heading "Don Quijote de la Mancha II", :sort-key "don quijote de la mancha"} {:heading "Get Carter : the screenplay", :sort-key "get carter: the screenplay"} {:heading "He jugado con lobos", :sort-key "he jugado con lobos"} {:heading "Hierro colado, acero moldeado y fundición maleable", :sort-key "hierro colado, acero moldeado y fundición maleable"} {:heading "El hijo del pirata", :sort-key "hijo del pirata"} {:heading "Jazzin' about [Música impresa] : fun pieces for clarinet [or] tenor sax and piano", :sort-key "jazzin' about: fun pieces for clarinet [or] tenor sax and piano"} {:heading "Juegos infantiles en nuestra sociedad tradicional", :sort-key "juegos infantiles en nuestra sociedad tradicional"} {:heading "Loreren ibilaldia", :sort-key "loreren ibilaldia"} {:heading "Motores de búsqueda e indexación", :sort-key "motores de búsqueda e indexación"} {:heading "Mundo del libro antiguo", :sort-key "mundo del libro antiguo"} {:heading "[Les noces de Cana] [Material gráfico] = [Las bodas de Caná]", :sort-key "noces de cana]= [las bodas de caná]"} {:heading "San Sebastián : un festival, una historia (1953-1966)", :sort-key "san sebastián: un festival, una historia (1953-1966)"} {:heading "Seven steps to heaven [Grabación sonora]", :sort-key "seven steps to heaven"} {:heading "Teleniños públicos", :sort-key "teleniños públicos"}]}

                   {:name "sample15", :headings [{:heading "1951-1952 decca recordings [Grabación sonora]", :sort-key "1951-1952 decca recordings"} {:heading "A de adulterio", :sort-key "a de adulterio"} {:heading "Antiguako jaiak : san juan 1988", :sort-key "antiguako jaiak: san juan 1988"} {:heading "El azúcar", :sort-key "azúcar"} {:heading "Chester Music for Viola [Música impresa]", :sort-key "chester music for viola"} {:heading "Colegio Oficial de Farmacéuticos de Guipúzcoa = Gipuzkoako Sendagaigileen Elkartea", :sort-key "colegio oficial de farmacéuticos de guipúzcoa= gipuzkoako sendagaigileen elkartea"} {:heading "La comunidad regular de Santa María de Roncesvalles (siglos XII-XIX)", :sort-key "comunidad regular de santa maría de roncesvalles (siglos xii-xix)"} {:heading "Un corazón en invierno [DVD-Vídeo]", :sort-key "corazón en invierno"} {:heading "Drogas endógenas : las drogas que produce nuestro cerebro", :sort-key "drogas endógenas: las drogas que produce nuestro cerebro"} {:heading "En elogio de la verdad", :sort-key "en elogio de la verdad"} {:heading "Escuela de belleza de Kabul", :sort-key "escuela de belleza de kabul"} {:heading "Exploradores del mar", :sort-key "exploradores del mar"} {:heading "Lecciones de navegación ó principios necesarios á la ciencia del piloto", :sort-key "lecciones de navegación ó principios necesarios á la ciencia del piloto"} {:heading "Pájinas escojidas : verso", :sort-key "pájinas escojidas: verso"} {:heading "El Retorno del halcón", :sort-key "retorno del halcón"} {:heading "Roberto Lesgues : exposición de pintura : Salas Municipales de Arte, San Sebastián, del 1 al 14 de Septiembre de 1960 [Folleto]", :sort-key "roberto lesgues: exposición de pintura : salas municipales de arte, san sebastián, del 1 al 14 de septiembre de 1960"} {:heading "Técnicas de grabación sonora", :sort-key "técnicas de grabación sonora"} {:heading "Todavía--", :sort-key "todavía--"} {:heading "Versos para niños : antología lírica ilustrada de poesías recitables", :sort-key "versos para niños: antología lírica ilustrada de poesías recitables"}]}

                   {:name "sample16", :headings [{:heading "Argentina", :sort-key "argentina"} {:heading "Cambio de estación", :sort-key "cambio de estación"} {:heading "Causes célébres du Droit des gens", :sort-key "causes célébres du droit des gens"} {:heading "La condition humaine", :sort-key "condition humaine"} {:heading "Controversias marítimas, intereses estatales y derecho internacional", :sort-key "controversias marítimas, intereses estatales y derecho internacional"} {:heading "Cuadernos de Lanzarote 2 : (1996-1997)", :sort-key "cuadernos de lanzarote 2: (1996-1997)"} {:heading "Desapareció una noche", :sort-key "desapareció una noche"} {:heading "Dios en persona", :sort-key "dios en persona"} {:heading "Dragones & mazmorras 3 [DVD-Vídeo]", :sort-key "dragones & mazmorras"} {:heading "Exposición Filatélica : homenaje a la mujer : San Sebastián, 27 de octubre al 2 de noviembre", :sort-key "exposición filatélica: homenaje a la mujer : san sebastián, 27 de octubre al 2 de noviembre"} {:heading "Fábulas", :sort-key "fábulas"} {:heading "Flores de Bach : restaura tu armonía interior", :sort-key "flores de bach: restaura tu armonía interior"} {:heading "Iván el Terrible", :sort-key "iván el terrible"} {:heading "Luis Marco : obras sobre papel y tela 1986-87 [Exposición. Sala I.B. Mixto 4, Zaragoza [Folleto]", :sort-key "luis marco: obras sobre papel y tela 1986-87 [exposición. sala i.b. mixto 4, zaragoza"} {:heading "La medición del mundo : un fascinante encuentro entre la literatura y la ciencia", :sort-key "medición del mundo: un fascinante encuentro entre la literatura y la ciencia"} {:heading "Obras completas de Cela 33", :sort-key "obras completas de cela 33"} {:heading "Performance [Grabación sonora]", :sort-key "performance"} {:heading "Rango [Grabación sonora]", :sort-key "rango"} {:heading "Sociología", :sort-key "sociología"} {:heading "Sucesos de historia literaria y civil", :sort-key "sucesos de historia literaria y civil"}]}

                   {:name "sample17"
                    :headings [{:heading "Apple" :sort-key "apple"}
                               {:heading "\"Orange\"" :sort-key "\"orange\""}]}

                   ])


(defn main []
  (let [tmpdir (Files/createTempDir)
        authority-index (file (doto (file tmpdir "authority")
                                .mkdirs)
                              "index")
        bib-index (file tmpdir "index")]
    (try

      (populate-index [] "authority" authority-index)

      (doseq [browse test-browses]
        (println "Loading browse headings:" (:name browse))
        (let [tmp-headings (File/createTempFile "vufind-browse-handler-tests" "")
              tmpdb (file tmpdir (str (:name browse) "_browse.db"))]
          (try
            (populate-index (:headings browse) (:name browse) bib-index)

            (doseq [prop ["bibleech" "sortfield" "valuefield"]]
              (System/clearProperty prop))

            (when (:sort-key (first (:headings browse)))
              (System/setProperty "bibleech" "StoredFieldLeech")
              (System/setProperty "sortfield" (str "sort-" (:name browse)))
              (System/setProperty "valuefield" (:name browse))
              )

            (PrintBrowseHeadings/main (into-array [(str bib-index)
                                                   (:name browse)
                                                   (str tmp-headings)]) )

            (CreateBrowseSQLite/main (into-array [(str tmp-headings)
                                                  (str tmpdb)]))
            (finally
             (.delete tmp-headings)))))


      (System/setProperty "solr.solr.home" (str tmpdir))

      (.mkdir (file tmpdir "collection1"))
      (.mkdir (file tmpdir "collection1" "conf"))

      (Files/copy (file "solr/solrconfig.xml")
                  (file tmpdir "collection1" "conf" "solrconfig.xml"))

      (Files/copy (file "solr/schema.xml")
                  (file tmpdir "collection1" "conf" "schema.xml"))


      (let [core (.initialize (new CoreContainer$Initializer))
            server (EmbeddedSolrServer. core "")]

        (println "\n====== Running tests ======\n")
        ;; Browse request!  finally...
        (is (=
             (do-browse server "author")
             ["AAA" "Äardvark" "Apple" "Banana" "grapefruit" "Orange"]))

        (is (=
             (do-browse server "title")
             ["AAA" "Äardvark" "Apple" "Banana" "grapefruit"
              "\"Hyphenated-words and double quotes\""
              "   inappropriate leading space"
              "Orange"]))

        (is (=
             (take 4 (do-browse server "title" {:from "App"}))
             ["Apple" "Banana" "grapefruit"
              "\"Hyphenated-words and double quotes\""]))

        (is (=
             (take 4 (do-browse server "title" {:from "Äard"}))
             ["Äardvark" "Apple" "Banana" "grapefruit"]))

        (is (=
             (take 3 (do-browse server "title" {:from "eggplant"}))
             ["grapefruit"
              "\"Hyphenated-words and double quotes\""
              "   inappropriate leading space"]))

        (is (=
             (take 4 (do-browse server "title" {:from "aardvark"}))
             ["Äardvark" "Apple" "Banana" "grapefruit"]))


        (doseq [browse test-browses]
          (when (.startsWith (:name browse)
                             "sample")
            (println "Test:" (:name browse))
            (is (=
                 (do-browse server (:name browse))
                 (map :heading (:headings browse))))))

        (println "\n====== Tests complete ======\n")

        (.shutdown core))



      (finally
        (rm-rf tmpdir)
        (shutdown-agents)))))

(main)

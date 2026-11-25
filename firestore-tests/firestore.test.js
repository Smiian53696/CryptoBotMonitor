
const { test, before, beforeEach, after } = require("node:test");

const {
    initializeTestEnvironment,
    assertSucceeds,
    assertFails
} = require("@firebase/rules-unit-testing");

const {
    doc,
    collection,
    query,
    where,
    getDoc,
    getDocs,
    setDoc,
    updateDoc,
    deleteDoc
} = require("firebase/firestore");

const fs = require("node:fs");
const path = require("node:path");

let testEnv;

const USER_1 = "user_1";
const USER_2 = "user_2";

// Przygotowanie danych alertu
function createAlert(id, userId, changes = {}) {
    return {
        id: id,
        userId: userId,
        coinId: "bitcoin",
        coinName: "Bitcoin",
        targetPrice: 70000,
        condition: "ABOVE",
        active: true,
        createdAt: Date.now(),
        ...changes
    };
}

// Przygotowanie danych ulubionej kryptowaluty
function createFavorite(userId, coinId, changes = {}) {
    return {
        id: `${userId}_${coinId}`,
        userId: userId,
        coinId: coinId,
        coinName: "Bitcoin",
        createdAt: Date.now(),
        ...changes
    };
}

// Pobranie bazy zalogowanego użytkownika
function getUserDb(userId) {
    return testEnv.authenticatedContext(userId).firestore();
}

// Pobranie bazy bez logowania
function getGuestDb() {
    return testEnv.unauthenticatedContext().firestore();
}

// Uruchomienie środowiska testowego
before(async () => {

    testEnv = await initializeTestEnvironment({
        projectId: "demo-cryptobotmonitor",

        firestore: {
            host: "127.0.0.1",
            port: 8080,

            rules: fs.readFileSync(
                path.join(__dirname, "../firestore.rules"),
                "utf8"
            )
        }
    });
});

// Przygotowanie bazy przed każdym testem
beforeEach(async () => {

    await testEnv.clearFirestore();

    await testEnv.withSecurityRulesDisabled(async (context) => {

        const db = context.firestore();

        // Aktywny alert użytkownika 1
        await setDoc(
            doc(db, "alerts", "alert_1"),
            createAlert("alert_1", USER_1)
        );

        // Aktywny alert użytkownika 2
        await setDoc(
            doc(db, "alerts", "alert_2"),
            createAlert("alert_2", USER_2)
        );

        // Wykonany alert użytkownika 1
        await setDoc(
            doc(db, "alerts", "alert_completed"),
            createAlert("alert_completed", USER_1, {
                active: false
            })
        );

        // Ulubione kryptowaluty
        await setDoc(
            doc(db, "favorites", "user_1_bitcoin"),
            createFavorite(USER_1, "bitcoin")
        );

        await setDoc(
            doc(db, "favorites", "user_2_bitcoin"),
            createFavorite(USER_2, "bitcoin")
        );

        // Dokument w niedostępnej kolekcji
        await setDoc(
            doc(db, "private_data", "test_document"),
            { value: "test" }
        );
    });
});

// Zamknięcie środowiska po testach
after(async () => {
    if (testEnv) {
        await testEnv.cleanup();
    }
});


// ======================================
// 1. AUTORYZACJA I DOSTĘP DO DANYCH
// ======================================

test("01 - Odczyt własnego alertu", async () => {

    const db = getUserDb(USER_1);

    await assertSucceeds(
        getDoc(doc(db, "alerts", "alert_1"))
    );
});

test("02 - Blokada odczytu cudzego alertu", async () => {

    const db = getUserDb(USER_2);

    await assertFails(
        getDoc(doc(db, "alerts", "alert_1"))
    );
});

test("03 - Blokada odczytu bez logowania", async () => {

    const db = getGuestDb();

    await assertFails(
        getDoc(doc(db, "alerts", "alert_1"))
    );
});

test("04 - Pobranie listy własnych alertów", async () => {

    const db = getUserDb(USER_1);

    const alertsQuery = query(
        collection(db, "alerts"),
        where("userId", "==", USER_1)
    );

    await assertSucceeds(
        getDocs(alertsQuery)
    );
});

test("05 - Blokada pobrania wszystkich alertów", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        getDocs(collection(db, "alerts"))
    );
});

test("06 - Blokada pobrania listy cudzych alertów", async () => {

    const db = getUserDb(USER_1);

    const alertsQuery = query(
        collection(db, "alerts"),
        where("userId", "==", USER_2)
    );

    await assertFails(
        getDocs(alertsQuery)
    );
});


// ======================================
// 2. TWORZENIE I WALIDACJA ALERTÓW
// ======================================

test("07 - Dodanie poprawnego alertu", async () => {

    const db = getUserDb(USER_1);

    await assertSucceeds(
        setDoc(
            doc(db, "alerts", "new_alert"),
            createAlert("new_alert", USER_1)
        )
    );
});

test("08 - Blokada tworzenia alertu dla innego użytkownika", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        setDoc(
            doc(db, "alerts", "new_alert"),
            createAlert("new_alert", USER_2)
        )
    );
});

test("09 - Blokada alertu z ujemną ceną", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        setDoc(
            doc(db, "alerts", "new_alert"),
            createAlert("new_alert", USER_1, {
                targetPrice: -100
            })
        )
    );
});

test("10 - Blokada alertu z ceną równą zero", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        setDoc(
            doc(db, "alerts", "new_alert"),
            createAlert("new_alert", USER_1, {
                targetPrice: 0
            })
        )
    );
});

test("11 - Blokada alertu ze zbyt wysoką ceną", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        setDoc(
            doc(db, "alerts", "new_alert"),
            createAlert("new_alert", USER_1, {
                targetPrice: 1000000000001
            })
        )
    );
});

test("12 - Blokada niepoprawnego warunku alertu", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        setDoc(
            doc(db, "alerts", "new_alert"),
            createAlert("new_alert", USER_1, {
                condition: "UNKNOWN"
            })
        )
    );
});

test("13 - Blokada dodawania dodatkowych pól", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        setDoc(
            doc(db, "alerts", "new_alert"),
            createAlert("new_alert", USER_1, {
                extraField: "test"
            })
        )
    );
});

test("14 - Blokada alertu bez wymaganego pola", async () => {

    const db = getUserDb(USER_1);

    const alert = createAlert("new_alert", USER_1);

    delete alert.createdAt;

    await assertFails(
        setDoc(
            doc(db, "alerts", "new_alert"),
            alert
        )
    );
});


// ======================================
// 3. AKTUALIZACJA I USUWANIE ALERTÓW
// ======================================

test("15 - Dezaktywacja własnego alertu", async () => {

    const db = getUserDb(USER_1);

    await assertSucceeds(
        updateDoc(
            doc(db, "alerts", "alert_1"),
            { active: false }
        )
    );
});

test("16 - Blokada ponownej aktywacji alertu", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        updateDoc(
            doc(db, "alerts", "alert_completed"),
            { active: true }
        )
    );
});

test("17 - Blokada zmiany właściciela alertu", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        updateDoc(
            doc(db, "alerts", "alert_1"),
            {
                userId: USER_2,
                active: false
            }
        )
    );
});

test("18 - Blokada zmiany ceny istniejącego alertu", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        updateDoc(
            doc(db, "alerts", "alert_1"),
            {
                targetPrice: 100000,
                active: false
            }
        )
    );
});

test("19 - Usunięcie własnego alertu", async () => {

    const db = getUserDb(USER_1);

    await assertSucceeds(
        deleteDoc(
            doc(db, "alerts", "alert_1")
        )
    );
});

test("20 - Blokada usunięcia cudzego alertu", async () => {

    const db = getUserDb(USER_2);

    await assertFails(
        deleteDoc(
            doc(db, "alerts", "alert_1")
        )
    );
});


// ======================================
// 4. ULUBIONE KRYPTOWALUTY
// ======================================

test("21 - Dodanie kryptowaluty do ulubionych", async () => {

    const db = getUserDb(USER_1);

    await assertSucceeds(
        setDoc(
            doc(db, "favorites", "user_1_ethereum"),
            createFavorite(USER_1, "ethereum", {
                coinName: "Ethereum"
            })
        )
    );
});

test("22 - Blokada dodawania ulubionych dla innego użytkownika", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        setDoc(
            doc(db, "favorites", "user_2_ethereum"),
            createFavorite(USER_2, "ethereum", {
                coinName: "Ethereum"
            })
        )
    );
});

test("23 - Blokada niepoprawnego ID dokumentu", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        setDoc(
            doc(db, "favorites", "wrong_id"),
            createFavorite(USER_1, "ethereum", {
                id: "wrong_id",
                coinName: "Ethereum"
            })
        )
    );
});

test("24 - Odczyt własnej ulubionej kryptowaluty", async () => {

    const db = getUserDb(USER_1);

    await assertSucceeds(
        getDoc(
            doc(db, "favorites", "user_1_bitcoin")
        )
    );
});

test("25 - Blokada odczytu cudzych ulubionych", async () => {

    const db = getUserDb(USER_2);

    await assertFails(
        getDoc(
            doc(db, "favorites", "user_1_bitcoin")
        )
    );
});

test("26 - Aktualizacja dozwolonych pól", async () => {

    const db = getUserDb(USER_1);

    await assertSucceeds(
        updateDoc(
            doc(db, "favorites", "user_1_bitcoin"),
            {
                coinName: "Bitcoin BTC",
                createdAt: Date.now()
            }
        )
    );
});

test("27 - Blokada zmiany coinId", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        updateDoc(
            doc(db, "favorites", "user_1_bitcoin"),
            {
                coinId: "ethereum"
            }
        )
    );
});

test("28 - Usunięcie własnej ulubionej kryptowaluty", async () => {

    const db = getUserDb(USER_1);

    await assertSucceeds(
        deleteDoc(
            doc(db, "favorites", "user_1_bitcoin")
        )
    );
});


// ======================================
// 5. POZOSTAŁE KOLEKCJE
// ======================================

test("29 - Blokada odczytu niedozwolonej kolekcji", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        getDoc(
            doc(db, "private_data", "test_document")
        )
    );
});

test("30 - Blokada zapisu do niedozwolonej kolekcji", async () => {

    const db = getUserDb(USER_1);

    await assertFails(
        setDoc(
            doc(db, "private_data", "new_document"),
            { value: "test" }
        )
    );
});

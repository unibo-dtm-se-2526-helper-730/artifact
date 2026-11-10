import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { setApiLanguage } from './api';
import type { Lang } from './types';

// Texts of the interface. Messages produced by the backend (errors, warnings,
// IVAFE notes, declaration items) arrive already translated and are not here.
const it = {
  'app.title': 'helper-730',
  'app.subtitle': '',
  'nav.import': 'Importa',
  'nav.instruments': 'Strumenti',
  'nav.transactions': 'Operazioni',
  'nav.summary': 'Riepilogo',
  'nav.declaration': 'Dichiarazione',
  'nav.ivafe': 'IVAFE',
  'nav.portfolio': 'Portafoglio',
  'year.label': "Anno d'imposta",
  'year.none': 'Nessun anno',
  'year.needed': 'Importa un file con operazioni fiscali per scegliere un anno.',
  'common.save': 'Salva',
  'common.saving': 'Salvataggio…',
  'common.saved': 'Salvato.',
  'common.cancel': 'Annulla',
  'common.loading': 'Caricamento…',
  'common.network': 'Il backend non risponde. È avviato su http://localhost:8080?',
  'common.empty': 'Nessun dato.',

  'import.title': 'Importa il CSV',
  'import.drop': 'Trascina qui il file CSV oppure',
  'import.choose': 'scegli un file',
  'import.uploading': 'Importazione di {name}…',
  'import.result': 'Importate {imported} · duplicate {duplicates} · righe con errori {errors}',
  'import.row': 'Riga',
  'import.severity': 'Tipo',
  'import.message': 'Messaggio',
  'import.ERROR': 'Errore',
  'import.WARNING': 'Avviso',
  'import.history': 'Storico degli import',
  'import.file': 'File',
  'import.date': 'Data',
  'import.imported': 'Importate',
  'import.duplicates': 'Duplicate',
  'import.errors': 'Errori',
  'import.danger': 'Elimina tutte le operazioni',
  'import.dangerText': 'Cancella tutte le operazioni importate e lo storico degli import.',
  'import.confirmTitle': 'Eliminare tutte le operazioni?',
  'import.confirmText': "L'operazione non si può annullare: dovrai reimportare i file.",
  'import.alsoInstruments': 'Elimina anche gli strumenti rimasti senza operazioni',
  'import.delete': 'Elimina',
  'import.deleted':
    'Eliminate {transactions} operazioni, {imports} import e {instruments} strumenti.',

  'instruments.title': 'Classifica gli strumenti',
  'instruments.intro': 'Il calcolo parte solo quando ogni strumento ha un tipo.',
  'instruments.ticker': 'Ticker',
  'instruments.name': 'Nome',
  'instruments.isin': 'ISIN',
  'instruments.class': 'Tipo',
  'instruments.unclassified': '— da classificare —',
  'instruments.govShare': 'Titoli di Stato %',
  'instruments.withholding': 'Ritenuta estera %',
  'instruments.status': 'Stato',
  'instruments.ok': 'Classificato',
  'instruments.todo': 'Da classificare',
  'instruments.empty': 'Nessuno strumento: importa prima un file.',

  'class.STOCK': 'Azione',
  'class.BOND': 'Obbligazione',
  'class.GOVERNMENT_BOND_WL': 'Titolo di Stato (white list)',
  'class.GOVERNMENT_BOND_NON_WL': 'Titolo di Stato (non white list)',
  'class.UCITS_ETF': 'ETF armonizzato',
  'class.NON_UCITS_ETF': 'ETF non armonizzato',
  'class.ETC_ETN': 'ETC / ETN',
  'class.null': 'Non classificato',

  'transactions.title': 'Operazioni del {year}',
  'transactions.date': 'Data',
  'transactions.type': 'Tipo',
  'transactions.quantity': 'Quantità',
  'transactions.price': 'Prezzo',
  'transactions.total': 'Importo',
  'op.BUY': 'Acquisto',
  'op.SELL': 'Vendita',
  'op.DIVIDEND': 'Dividendo',
  'op.COUPON': 'Cedola',

  'summary.title': 'Riepilogo {year}',
  'summary.lost': 'Hai perso {amount}',
  'summary.gained': 'Hai guadagnato {amount}',
  'summary.owe': 'Devi versare {amount}',
  'summary.belowThreshold': 'Imposta di {amount}: sotto la soglia di {threshold}, non si versa.',
  'summary.nothing': 'Nessuna imposta da versare.',
  'summary.economic': 'Risultato economico del {year}',
  'summary.misc': 'Redditi diversi',
  'summary.miscHint': 'Plusvalenze e minusvalenze compensabili con lo Zainetto',
  'summary.capital': 'Redditi di capitale',
  'summary.capitalHint': 'Dividendi, cedole e plusvalenze su ETF armonizzati: mai compensabili',
  'summary.taxable': 'Imponibile',
  'summary.tax': 'Imposta ({rate}%)',
  'summary.zainetto': 'Zainetto fiscale',
  'summary.zainettoTotal': 'Disponibile a fine anno',
  'summary.used': "Usato quest'anno",
  'summary.expires': 'del {year}: {amount}, utilizzabile fino al 31/12/{last}',
  'summary.zainettoEmpty': 'Lo Zainetto è vuoto.',
  'summary.unclassified': 'Per calcolare il {year} classifica prima questi strumenti: {tickers}.',
  'summary.goInstruments': 'Vai a Strumenti',
  'summary.events': 'Da dove viene ogni euro',
  'summary.eventsHint':
    'Clicca una vendita per vedere i lotti di acquisto che ha consumato (LIFO).',
  'summary.category': 'Categoria',
  'summary.amount': 'Importo',
  'summary.rate': 'Aliquota',
  'summary.taxableAmount': 'Imponibile',
  'cat.MISCELLANEOUS': 'Reddito diverso',
  'cat.CAPITAL': 'Reddito di capitale',
  'cat.EXCLUDED': 'Escluso',
  'event.CAPITAL_GAIN': 'Plusvalenza',
  'event.CAPITAL_LOSS': 'Minusvalenza',
  'event.DIVIDEND': 'Dividendo',
  'event.COUPON': 'Cedola',
  'lots.acquired': 'Acquistato il',
  'lots.quantity': 'Quote',
  'lots.cost': 'Costo',
  'lots.proceeds': 'Ricavo',
  'lots.gain': 'Risultato',
  'carried.title': 'Minusvalenze degli anni precedenti',
  'carried.intro': 'Perdite realizzate prima del primo import: entrano nello Zainetto iniziale.',
  'carried.year': 'Anno',
  'carried.amount': 'Importo (€)',
  'carried.add': 'Aggiungi anno',
  'carried.remove': 'Rimuovi',

  'declaration.title': "Modello {model} - anno d'imposta {year}",
  'declaration.rigo': 'Rigo',
  'declaration.what': 'Voce',
  'declaration.amount': 'Importo',
  'declaration.detail': 'Altre colonne',
  'declaration.secondary': 'Importo collegato',
  'declaration.disclaimer':
    "Prospetto di aiuto alla compilazione: verifica sempre i righi sulle istruzioni ufficiali dell'anno.",
  'declaration.empty': "Nessun quadro da compilare per quest'anno.",

  'ivafe.title': 'IVAFE {year}',
  'ivafe.total': 'IVAFE totale',
  'ivafe.below': 'Sotto la soglia di {threshold}: non si versa.',
  'ivafe.missing': 'Mancano i prezzi al 31/12 di: {tickers}. Inseriscili qui sotto.',
  'ivafe.ticker': 'Titolo',
  'ivafe.period': 'Periodo',
  'ivafe.days': 'Giorni',
  'ivafe.initial': 'Valore iniziale',
  'ivafe.final': 'Valore finale',
  'ivafe.amount': 'IVAFE',
  'ivafe.notes': 'Note',
  'ivafe.prices': 'Prezzi al 31/12',
  'ivafe.pricesIntro': "Prezzo di chiusura dell'ultimo giorno di borsa dell'anno.",
  'ivafe.savePrices': 'Salva i prezzi e ricalcola',

  'portfolio.title': 'Portafoglio attuale',
  'portfolio.intro': 'Posizioni aperte valutate al costo di carico.',
  'portfolio.byTicker': 'Per titolo',
  'portfolio.byClass': 'Per tipo',
  'portfolio.total': 'Totale investito',
  'portfolio.quantity': 'Quote',
  'portfolio.cost': 'Costo',
  'portfolio.average': 'Costo medio',
  'portfolio.weight': 'Peso',
  'portfolio.other': 'Altro',
  'portfolio.empty': 'Nessuna posizione aperta.',
};

type Key = keyof typeof it;

const en: Record<Key, string> = {
  'app.title': 'helper-730',
  'app.subtitle': '',
  'nav.import': 'Import',
  'nav.instruments': 'Instruments',
  'nav.transactions': 'Transactions',
  'nav.summary': 'Summary',
  'nav.declaration': 'Declaration',
  'nav.ivafe': 'IVAFE',
  'nav.portfolio': 'Portfolio',
  'year.label': 'Tax year',
  'year.none': 'No year',
  'year.needed': 'Import a file with taxable operations to choose a year.',
  'common.save': 'Save',
  'common.saving': 'Saving…',
  'common.saved': 'Saved.',
  'common.cancel': 'Cancel',
  'common.loading': 'Loading…',
  'common.network': 'The backend does not answer. Is it running on http://localhost:8080?',
  'common.empty': 'No data.',

  'import.title': 'Import the CSV',
  'import.drop': 'Drop the CSV file here or',
  'import.choose': 'choose a file',
  'import.uploading': 'Importing {name}…',
  'import.result': 'Imported {imported} · duplicates {duplicates} · rows with errors {errors}',
  'import.row': 'Row',
  'import.severity': 'Type',
  'import.message': 'Message',
  'import.ERROR': 'Error',
  'import.WARNING': 'Warning',
  'import.history': 'Import history',
  'import.file': 'File',
  'import.date': 'Date',
  'import.imported': 'Imported',
  'import.duplicates': 'Duplicates',
  'import.errors': 'Errors',
  'import.danger': 'Delete all transactions',
  'import.dangerText': 'Deletes every imported transaction and the import history.',
  'import.confirmTitle': 'Delete all transactions?',
  'import.confirmText': 'This cannot be undone: you will have to import the files again.',
  'import.alsoInstruments': 'Also delete the instruments left without transactions',
  'import.delete': 'Delete',
  'import.deleted':
    'Deleted {transactions} transactions, {imports} imports and {instruments} instruments.',

  'instruments.title': 'Classify the instruments',
  'instruments.intro': 'The calculation starts only when every instrument has a type.',
  'instruments.ticker': 'Ticker',
  'instruments.name': 'Name',
  'instruments.isin': 'ISIN',
  'instruments.class': 'Type',
  'instruments.unclassified': '— to classify —',
  'instruments.govShare': 'Government bonds %',
  'instruments.withholding': 'Foreign withholding %',
  'instruments.status': 'Status',
  'instruments.ok': 'Classified',
  'instruments.todo': 'To classify',
  'instruments.empty': 'No instruments: import a file first.',

  'class.STOCK': 'Stock',
  'class.BOND': 'Bond',
  'class.GOVERNMENT_BOND_WL': 'Government bond (white list)',
  'class.GOVERNMENT_BOND_NON_WL': 'Government bond (non white list)',
  'class.UCITS_ETF': 'UCITS ETF',
  'class.NON_UCITS_ETF': 'Non-UCITS ETF',
  'class.ETC_ETN': 'ETC / ETN',
  'class.null': 'Unclassified',

  'transactions.title': 'Transactions of {year}',
  'transactions.date': 'Date',
  'transactions.type': 'Type',
  'transactions.quantity': 'Quantity',
  'transactions.price': 'Price',
  'transactions.total': 'Amount',
  'op.BUY': 'Buy',
  'op.SELL': 'Sell',
  'op.DIVIDEND': 'Dividend',
  'op.COUPON': 'Coupon',

  'summary.title': 'Summary {year}',
  'summary.lost': 'You lost {amount}',
  'summary.gained': 'You gained {amount}',
  'summary.owe': 'You owe {amount}',
  'summary.belowThreshold': 'Tax of {amount}: below the {threshold} threshold, nothing to pay.',
  'summary.nothing': 'No tax to pay.',
  'summary.economic': 'Economic result of {year}',
  'summary.misc': 'Miscellaneous income',
  'summary.miscHint': 'Gains and losses that can be offset with the Zainetto',
  'summary.capital': 'Capital income',
  'summary.capitalHint': 'Dividends, coupons and gains on UCITS ETFs: never offset',
  'summary.taxable': 'Taxable',
  'summary.tax': 'Tax ({rate}%)',
  'summary.zainetto': 'Zainetto',
  'summary.zainettoTotal': 'Available at year end',
  'summary.used': 'Used this year',
  'summary.expires': 'from {year}: {amount}, usable until 31/12/{last}',
  'summary.zainettoEmpty': 'The Zainetto is empty.',
  'summary.unclassified': 'To compute {year}, first classify these instruments: {tickers}.',
  'summary.goInstruments': 'Go to Instruments',
  'summary.events': 'Where every euro comes from',
  'summary.eventsHint': 'Click a sale to see the purchase lots it consumed (LIFO).',
  'summary.category': 'Category',
  'summary.amount': 'Amount',
  'summary.rate': 'Rate',
  'summary.taxableAmount': 'Taxable',
  'cat.MISCELLANEOUS': 'Miscellaneous',
  'cat.CAPITAL': 'Capital',
  'cat.EXCLUDED': 'Excluded',
  'event.CAPITAL_GAIN': 'Capital gain',
  'event.CAPITAL_LOSS': 'Capital loss',
  'event.DIVIDEND': 'Dividend',
  'event.COUPON': 'Coupon',
  'lots.acquired': 'Bought on',
  'lots.quantity': 'Units',
  'lots.cost': 'Cost',
  'lots.proceeds': 'Proceeds',
  'lots.gain': 'Result',
  'carried.title': 'Losses from previous years',
  'carried.intro': 'Losses realized before the first import: they open the Zainetto.',
  'carried.year': 'Year',
  'carried.amount': 'Amount (€)',
  'carried.add': 'Add year',
  'carried.remove': 'Remove',

  'declaration.title': 'Modello {model} - tax year {year}',
  'declaration.rigo': 'Rigo',
  'declaration.what': 'Item',
  'declaration.amount': 'Amount',
  'declaration.detail': 'Other columns',
  'declaration.secondary': 'Related amount',
  'declaration.disclaimer':
    'A filing aid: always check the rows against the official instructions of the year.',
  'declaration.empty': 'No quadro to fill in for this year.',

  'ivafe.title': 'IVAFE {year}',
  'ivafe.total': 'Total IVAFE',
  'ivafe.below': 'Below the {threshold} threshold: nothing to pay.',
  'ivafe.missing': 'Prices at 31/12 are missing for: {tickers}. Enter them below.',
  'ivafe.ticker': 'Security',
  'ivafe.period': 'Period',
  'ivafe.days': 'Days',
  'ivafe.initial': 'Initial value',
  'ivafe.final': 'Final value',
  'ivafe.amount': 'IVAFE',
  'ivafe.notes': 'Notes',
  'ivafe.prices': 'Prices at 31/12',
  'ivafe.pricesIntro': 'Closing price of the last trading day of the year.',
  'ivafe.savePrices': 'Save prices and recompute',

  'portfolio.title': 'Current portfolio',
  'portfolio.intro': 'Open positions valued at cost.',
  'portfolio.byTicker': 'By security',
  'portfolio.byClass': 'By type',
  'portfolio.total': 'Total invested',
  'portfolio.quantity': 'Units',
  'portfolio.cost': 'Cost',
  'portfolio.average': 'Average cost',
  'portfolio.weight': 'Weight',
  'portfolio.other': 'Other',
  'portfolio.empty': 'No open positions.',
};

const dictionaries: Record<Lang, Record<Key, string>> = { it, en };

export type Translate = (key: Key, vars?: Record<string, string | number>) => string;

interface I18n {
  lang: Lang;
  setLang: (lang: Lang) => void;
  t: Translate;
}

const I18nContext = createContext<I18n | null>(null);

/** localStorage can be unavailable (privacy modes): the app must work anyway. */
function storedLang(): Lang {
  try {
    const saved = localStorage.getItem('lang');
    if (saved === 'it' || saved === 'en') return saved;
  } catch {
    /* ignore */
  }
  return navigator.language.toLowerCase().startsWith('it') ? 'it' : 'en';
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLang] = useState<Lang>(storedLang);

  // keep the backend language, the <html lang> attribute and the saved choice in sync
  setApiLanguage(lang);
  useEffect(() => {
    document.documentElement.lang = lang;
    try {
      localStorage.setItem('lang', lang);
    } catch {
      /* ignore */
    }
  }, [lang]);

  const t: Translate = (key, vars) =>
    dictionaries[lang][key].replace(/\{(\w+)\}/g, (_, name: string) =>
      String(vars?.[name] ?? `{${name}}`),
    );

  return <I18nContext.Provider value={{ lang, setLang, t }}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18n {
  const context = useContext(I18nContext);
  if (!context) throw new Error('useI18n outside I18nProvider');
  return context;
}

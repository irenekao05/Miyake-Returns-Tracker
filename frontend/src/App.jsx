import { useEffect, useState, useRef } from 'react'
import { RefreshCw, Undo2, Trash } from 'lucide-react';
import errorSoundUrl from './assets/uh-oh.mp3';

// backend API URL 
const API_URL = `${import.meta.env.BASE_URL}api`;


function App() {
  /* ==========================================
     STATE
  ========================================== */

  // Location state
  const [locations, setLocations] = useState([]);
  const [locationLoadError, setLocationLoadError] = useState('');
  const [selectedLocationId, setSelectedLocationId] = useState(null);

  // Product state
  const [products, setProducts] = useState({});
  const [isLoadingProducts, setIsLoadingProducts] = useState(true);
  const [productLoadError, setProductLoadError] = useState('');

  // Scan mode (return or production)
  const [scanMode, setScanMode] = useState(null);

  // Scan state
  const [barcode, setBarcode] = useState('');
  const [unknownBarcode, setUnknownBarcode] = useState(null);
  const [counts, setCounts] = useState({});
  const [scanHistory, setScanHistory] = useState([]);
  const [scanResult, setScanResult] = useState(null);
  const barcodeInputRef = useRef(null);
  const errorAudioRef = useRef(null);

  // Save state
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [saveSuccess, setSaveSuccess] = useState('');
  
  // Login state
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [pinInput, setPinInput] = useState('');
  const [pinError, setPinError] = useState('');
  const [isCheckingPin, setIsCheckingPin] = useState(false);
  const [csrfToken, setCsrfToken] = useState(null);

  /* ==========================================
     EVENT HANDLERS
  ========================================== */

  function handleBarcodeEnter(event) {
    if (event.key === 'Enter') {
      if (isSaving || unknownBarcode !== null) {
        return; 
      }

      setSaveSuccess('');

      const scannedBarcode = barcode.trim();

      if (scannedBarcode === '') {
        return;
      }

      const productName = products[scannedBarcode];
      if (productName) {
        // increment individual product counts 
        setCounts(previousCounts => {
          return {
            ...previousCounts,
            [scannedBarcode]: (previousCounts[scannedBarcode] ?? 0) + 1
          };
        });

        // update history with every new scan
        setScanHistory(previousHistory => {
          return [...previousHistory, scannedBarcode];
        });

        // save succesful scan status
        setScanResult({
          type: 'success',
          message: productName,
        });
      }
      else {
        setScanResult({
          type: 'error',
          message: 'Unknown barcode: ' + scannedBarcode,
        });

        setUnknownBarcode(scannedBarcode);
        playErrorSound();
      }

      setBarcode('');
    }
  }


  function resetCounts() {
    setScanResult(null);
    setCounts({});
    setScanHistory([]);
  }
  

  function undoLastScan() {
    if (scanHistory.length === 0) {
      return;
    }

    const lastBarcode = scanHistory.at(-1)

    setCounts(previousCounts => {
      const decrementedCounts = {...previousCounts};

      // decrement product count or delete product
      if (decrementedCounts[lastBarcode] > 1) {
        decrementedCounts[lastBarcode] -= 1;
      }
      else {
        delete decrementedCounts[lastBarcode];
      }

      return decrementedCounts;
    });

    // delete last item in scan history
    setScanHistory(previousHistory => {
      return previousHistory.slice(0, -1)
    });

    setScanResult(null);
  }

  
  function changeLocation() {
    if (Object.keys(counts).length > 0 && !window.confirm('Changing locations will discard all current scanned items. Continue?')) {
      return;
    }

    setCounts({});
    setScanMode(null);
    setScanHistory([]);
    setScanResult(null);
    setBarcode('');
    setProducts({});
    setProductLoadError('');
    setIsLoadingProducts(false);
    setSelectedLocationId(null);
    setSaveError('');
    setSaveSuccess('');
  }


  function selectLocation(locationId) {
    setIsLoadingProducts(true);
    setProductLoadError('');
    setProducts({});
    setSelectedLocationId(locationId);
  }


  async function playErrorSound() {
    try {
      if (errorAudioRef.current === null) {
        errorAudioRef.current = new Audio(errorSoundUrl);
      }

      const audio = errorAudioRef.current;
      audio.currentTime = 0;

      await audio.play();
    } 
    catch (error) {
      console.warn('Could not play the error sound:', error);
    }
  }


  async function saveReturns() {
    if (Object.keys(counts).length === 0 || isSaving) {
      return;
    }

    if (!isAuthenticated || csrfToken === null) {
      setSaveError('Please log in before saving.');
      return;
    }
    
    setIsSaving(true);
    setSaveError('');
    setSaveSuccess('');

    try {
      const response = await fetch(`${API_URL}/saveReturns`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json', [csrfToken.headerName]: csrfToken.token},
        body: JSON.stringify({locationId: selectedLocationId, mode: scanMode, scannedCounts: counts}),
      });

      if (response.status === 401 || response.status === 403) {
        setIsAuthenticated(false);
        setCsrfToken(null);
        setPinInput('');
        setPinError('Please enter your PIN again.');

        return;
      }

      if (!response.ok) {
        throw new Error('Could not save returns');
      }

      const data = await response.json();

      if (data.sheetsSaved !== true) {
        throw new Error('Save could not be confirmed. Check spreadsheet before retrying');
      }

      resetCounts();

      if (data.databaseSaved === true) {
        setSaveSuccess('Counts saved successfully.');
      } else {
        setSaveError('Saved to Google Sheets, but database saving failed.\n' + 'Do not resubmit these counts. Please contact admin.');
      }
    }
    catch (error) {
      setSaveError(error.message);
    }
    finally {
      setIsSaving(false);
    }
  }


  async function loadLocations() {
    setLocationLoadError('');

    try {
      const response = await fetch(`${API_URL}/locations`);

      if (response.status === 401) {
        setIsAuthenticated(false);
        setCsrfToken(null);
        setPinInput('');
        setPinError('Please enter your PIN again.');

        return;
      }

      if (!response.ok) {
        throw new Error('Could not load locations\nPlease contact admin');
      }

      const data = await response.json();
      setLocations(data.locations);
    }
    catch (error) {
      setLocationLoadError(error.message);
    }
  }


  async function fetchProducts(locationId, signal) {
    const response = await fetch(`${API_URL}/products?locationId=${locationId}`, {signal});

    if (response.status === 401) {
      setIsAuthenticated(false);
      setCsrfToken(null);
      setPinInput('');
      setPinError('Please enter your PIN again.');

      return null;
    }

    if (!response.ok) {
      throw new Error('Please contact admin');
    }

    const data = await response.json();
    return data.products;
  }


  async function getCsrfToken() {
    const response = await fetch(`${API_URL}/csrf`);

    if (!response.ok) {
      throw new Error('Could not prepare login. Please try again.');
    }

    return await response.json();
  }


  async function handlePinSubmit(event) {
    event.preventDefault();

    if (isCheckingPin) {
      return;
    }

    setIsCheckingPin(true);
    setPinError('');

    try {
      const loginToken = await getCsrfToken();

      const formData = new URLSearchParams();
      formData.set('username', 'staff');
      formData.set('password', pinInput);

      const response = await fetch(`${API_URL}/authenticate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          [loginToken.headerName]: loginToken.token,
        },
        body: formData.toString(),
      });

      if (response.status === 401) {
        throw new Error('Incorrect PIN. Please try again.');
      }

      if (!response.ok) {
        throw new Error('Could not log in. Please try again.');
      }

      const newToken = await getCsrfToken();

      setCsrfToken(newToken);
      setPinInput('');
      setIsAuthenticated(true);
    } 
    catch (error) {
      setPinError(error.message);
      setPinInput('');
    } 
    finally {
      setIsCheckingPin(false);
    }
  }

  /* ==========================================
    CALCULATED VALUES
  ========================================== */
  
  // calculate total returns count 
  let totalReturns = 0;

  for (const quantity of Object.values(counts)) {
    totalReturns += quantity;
  }

  // calculate unique returns count
  const uniqueReturns = Object.keys(counts).length;

  // calculate scan success/failure notice values
  let notificationTitle = '';
  let notificationBoxClasses = '';
  let notificationTitleClasses = '';
  let notificationMessageClasses = '';

  if (scanResult !== null) {
    if (scanResult.type === 'success') {
      notificationTitle = 'Scanned successfully!';
      notificationBoxClasses = 'bg-green-100 border-green-400';
      notificationTitleClasses = 'text-green-800';
      notificationMessageClasses = 'text-green-700';
    } 
    else {
      notificationTitle = 'Barcode not recognized';
      notificationBoxClasses = 'bg-red-100 border-red-400';
      notificationTitleClasses = 'text-red-800';
      notificationMessageClasses = 'text-red-700';
    }
  }

  // calculate the marketplace name from chosen locationId
  let selectedLocation = null;

  for (const location of locations) {
    if (location.id === selectedLocationId) {
      selectedLocation = location;
      break;
    }
  }

  // calculate scan section title from chosen scanMode
  let totalLabel = 'Total Returns';
  let listTitle = "Today's Returns";

  if (scanMode === 'PRODUCTION') {
    totalLabel = 'Total Produced';
    listTitle = "Today's Production";
  }
    

  /* ==========================================
     EFFECTS
  ========================================== */

  // load locations
  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }

    loadLocations();
  }, [isAuthenticated]);


  // fetching products for selected location
  useEffect(() => {
    if (!isAuthenticated || selectedLocationId === null) {
      return;
    }

    const controller = new AbortController();
    setIsLoadingProducts(true);
    setProductLoadError('');
    setProducts({});

    async function load() {
      try {
        const products = await fetchProducts(selectedLocationId, controller.signal);

        if (controller.signal.aborted || products === null) {
          return;
        }
        setProducts(products);
      } 
      catch (error) {
        if (!controller.signal.aborted) {
          setProductLoadError(error.message);
        }
      } 
      finally {
        if (!controller.signal.aborted) {
          setIsLoadingProducts(false);
        }
      }
    }

    load();

    return () => {
      controller.abort();
    }

  }, [selectedLocationId, isAuthenticated]);


  // scan success or failure notice timeout
  useEffect(()=> {
    if (scanResult === null) {
      return;
    }

    const timer = setTimeout(() => {
      setScanResult(null);
    }, 2000);

    return () => {
      clearTimeout(timer);
    };
  }, [scanResult]);


  // save success notice timeout
  useEffect(() => {
    if (saveSuccess === '') {
      return;
    }

    const timer = setTimeout(() => {
      setSaveSuccess('');
    }, 4000);

    return () => {
      clearTimeout(timer);
    }
  }, [saveSuccess]);


  // block keyboard input when unrecognized barcode is scanned
  useEffect(() => {
    if (!isAuthenticated || unknownBarcode === null) {
      return;
    }

    function blockKeyboard(event) {
      event.preventDefault();
      event.stopPropagation();
    }

    window.addEventListener('keydown', blockKeyboard, true);

    return () => {
      window.removeEventListener('keydown', blockKeyboard, true);
    };
  }, [unknownBarcode, isAuthenticated]);

  
  // focuses barcode input text box
  useEffect(() => {
    if (unknownBarcode === null && !isSaving) {
      barcodeInputRef.current?.focus();
    }
  }, [unknownBarcode, isSaving]);

  
  // redirect user to login screen after 30 minutes of inactivity
  useEffect(() => {
    if (!isAuthenticated || isSaving) {
      return;
    }

    let timer;

    function lockScreen() {
      setIsAuthenticated(false);
      setCsrfToken(null);
      setPinInput('');
      setPinError('You were inactive for 30 minutes. Please enter your PIN again.');
    }

    function restartTimer() {
      clearTimeout(timer);
      timer = setTimeout(lockScreen, 30 * 60 * 1000);
    }

    restartTimer();

    window.addEventListener('keydown', restartTimer, true);
    window.addEventListener('pointerdown', restartTimer, true);

    return () => {
      clearTimeout(timer);
      window.removeEventListener('keydown', restartTimer, true);
      window.removeEventListener('pointerdown', restartTimer, true);
    };
  }, [isAuthenticated, isSaving]);


  /* ==========================================
     LOADING SCREENS
  ========================================== */

  // pin screen
  if (!isAuthenticated) {
    return (
      <main className='flex min-h-dvh overflow-y-auto items-center justify-center bg-indigo-50 p-4'>
        <form
          onSubmit={handlePinSubmit}
          className='flex flex-col items-center w-full max-w-sm rounded-lg bg-white p-6 shadow-lg'
        >
          <h1 className='text-center text-2xl font-bold text-slate-800'>Sushi & Bento Tracker</h1>

          <label htmlFor='pin' className='mt-6 block text-slate-600'>Enter PIN</label>

          <input
            id='pin'
            type='password'
            inputMode='numeric'
            maxLength={4}
            value={pinInput}
            onChange={(event) => setPinInput(event.target.value)}
            disabled={isCheckingPin}
            autoFocus
            className='mt-2 w-full rounded-lg border-2 border-indigo-300 p-3 text-center text-xl'
          />

          {pinError !== '' && (
            <p role='alert' className='mt-3 text-sm text-red-600'>
              {pinError}
            </p>
          )}

          <button
            type='submit'
            disabled={isCheckingPin || pinInput.length !== 4}
            className='mt-4 w-full rounded-lg bg-indigo-600 p-3 font-semibold text-white disabled:opacity-50'
          >
            Login
          </button>
        </form>
      </main>
    );
  }
  
  // location selector screen
  if (selectedLocationId === null) {
    return (
      <main className='flex min-h-dvh items-center justify-center bg-linear-to-br from-blue-50 to-indigo-100 p-4'>
        <section className='w-full max-w-lg rounded-2xl bg-white p-6 shadow-lg sm:p-8'>
          <h1 className='text-center text-2xl font-bold text-slate-800'>Select a Location</h1>

          <p className='mt-2 text-center text-slate-500'>Choose the store you are scanning returns for</p>

          {locationLoadError !== '' && (
            <p role='alert' className='mt-4 text-center text-red-600 whitespace-pre-line'>{locationLoadError}</p>
          )}

          <div className='mt-6 flex flex-col gap-3'>
            {locations.map((location) => {
              return (
                <button
                  key={location.id}
                  type='button'
                  onClick={() => selectLocation(location.id)}
                  className='w-full rounded-xl border border-indigo-200 bg-indigo-50 px-5 py-4 text-left 
                             transition-colors hover:border-indigo-400 hover:bg-indigo-100
                             focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600
                             flex flex-col items-center'
                >
                  <span className='block text-lg font-semibold text-slate-800'>
                    {location.marketplaceName}
                  </span>

                  <span className='mt-1 block text-sm text-slate-500'>
                    {location.locationName}
                  </span>
                </button>
              );
            })}
          </div>
        </section>
      </main>
    )
  }


  // scan mode selector screen 
  if (scanMode === null) {
    return (
      <main className='flex min-h-dvh items-center justify-center bg-linear-to-br from-blue-50 to-indigo-100 p-4'>
        <section className='w-full max-w-lg rounded-2xl bg-white p-6 shadow-lg sm:p-8'>
          <h1 className='text-center text-2xl font-bold text-slate-800'>What are you scanning?</h1>

          <div className='mt-6 flex flex-col gap-3'>
            <button
              type='button'
              onClick={() => setScanMode('PRODUCTION')}
              className='rounded-xl border border-indigo-200 bg-indigo-50 px-5 py-4 text-center hover:bg-indigo-100'
            >
              <span className='block text-lg font-semibold text-slate-800'>Production</span>
              <span className='mt-1 block text-sm text-slate-500'>Products made today</span>
            </button>

            <button
              type='button'
              onClick={() => setScanMode('RETURNS')}
              className='rounded-xl border border-indigo-200 bg-indigo-50 px-5 py-4 text-center hover:bg-indigo-100'
            >
              <span className='block text-lg font-semibold text-slate-800'>Returns</span>
              <span className='mt-1 block text-sm text-slate-500'>Unsold products from yesterday</span>
            </button>
          </div>
        </section>
      </main>
    );
  }


  // loading products screen 
  if (isLoadingProducts || productLoadError !== '') {
    return (
      <main className='flex min-h-dvh items-center justify-center bg-linear-to-br from-blue-50 to-indigo-100 p-4'>
        <div className='mx-auto w-full max-w-sm rounded-lg bg-white px-6 py-8 text-center shadow-lg'>
          {productLoadError !== '' ? (
            <div role='alert'>
              <h1 className='text-2xl font-bold text-red-800'>Could not load products</h1>

              <p className='mt-3 whitespace-pre-line font-light leading-relaxed text-slate-600'>{productLoadError}</p>
            </div>
          ) : (
            <div role='status'>
              <RefreshCw aria-hidden='true' className='mx-auto mb-6 h-16 w-16 text-indigo-600 motion-safe:animate-spin'/>

              <h1 className='text-2xl font-bold text-slate-800'>Loading Products...</h1>

              <p className='mt-3 font-light leading-relaxed text-slate-600'>Please wait while we fetch your product list</p>
            </div>
          )}
        </div>
      </main>
    );
  }


  /* ==========================================
     PAGE LAYOUT
  ========================================== */
  return (
    <main className='bg-indigo-50 h-dvh overflow-y-auto p-4'>
      <div className='flex h-full min-h-0 w-full max-w-2xl mx-auto flex-col'>

        {/* TITLE + COUNT CARD */}
        <header className='bg-white rounded-lg p-4 shadow-md shrink-0'>
          <h1 className='text-xl font-bold text-slate-800'>Sushi & Bento Tracker</h1>

          {selectedLocation && (
            <button
              type='button'
              onClick={changeLocation}
              disabled={isSaving}
              className='inline-flex shrink-0 items-center  gap-2 rounded-lg  text-sm text-indigo-600 hover:underline
                        disabled:cursor-not-allowed disabled:opacity-50'
            >
              
              <span className='text-left'>
                {selectedLocation.marketplaceName}
                {' · '}
                {selectedLocation.locationName.split(',')[0].trim()}
              </span>
              <RefreshCw size={14} aria-hidden='true' />
            </button>
          )}
         
          <div className='grid grid-cols-2 mt-4 gap-3'>
            {/* total return count */}
            <div className='bg-blue-50 rounded-lg p-3'>
              <p className='text-xs font-semibold text-blue-500'>{totalLabel}</p>
              <p className='text-2xl font-bold text-blue-600 mt-1'>{totalReturns}</p>
            </div>

            {/* total count of scanned unique product IDs */}
            <div className='bg-green-50 rounded-lg p-3'>
              <p className='text-xs font-semibold text-green-600'>Unique Products</p>
              <p className='text-2xl font-bold text-green-600 mt-1'>{uniqueReturns}</p>
            </div>
          </div>
        </header>
        

        {/* SCAN BARCODE CARD */}
        <section className='bg-white rounded-lg p-4 shadow-md mt-4 shrink-0'>
          <div className='flex items-center justify-between'>
            <h2 className='text-lg font-semibold text-slate-800'>Barcode Scanner</h2>

            <span className='text-sm text-green-600/70 flex items-center gap-2'>
              <span className='rounded-full bg-green-600/70 h-2 w-2'/>
              Ready
            </span>
          </div>
          
          {/* barcode input text field */}
          <label htmlFor='barcode' className='sr-only'>barcode</label>
          <input 
            type='text' 
            id='barcode' 
            placeholder='Scan barcode here...'
            disabled={isSaving || unknownBarcode !== null}
            className='text-base border-indigo-400 border-2 rounded-lg placeholder:text-slate-400 w-full p-3 mt-3'
            value={barcode}
            onChange={(event) => setBarcode(event.target.value)}
            onKeyDown={handleBarcodeEnter}
            ref={barcodeInputRef}
            autoFocus
          />
        </section>


        {/* SCAN SUCCESS NOTICE */}
        {scanResult !== null && (
          <div
            role='status'
            className={'mt-4 shrink-0 rounded-lg border p-4 ' + notificationBoxClasses}
          >
            <p className={'font-semibold ' + notificationTitleClasses}>
              {notificationTitle}
            </p>

            <p className={'mt-1 text-sm font-light ' + notificationMessageClasses}>
              {scanResult.message}
            </p>
          </div>
        )}


        {/* SCANNED RETURNS LIST CARD */}
        <section className='bg-white rounded-lg p-4 shadow-md mt-4 flex min-h-64 flex-1 flex-col'>
          <div className='flex flex-wrap shrink-0 items-center justify-between gap-1'>
            <h2 className='text-lg font-semibold text-slate-800'>{listTitle}</h2>
            
            <div className='flex gap-2'>
              <button type='button' 
                disabled={isSaving || scanHistory.length === 0}
                onClick={undoLastScan}
                className='flex items-center gap-1 bg-orange-100 rounded-lg text-orange-700 text-sm 
                           disabled:opacity-50 disabled:cursor-not-allowed px-3 py-2'
              >
                <Undo2 aria-hidden='true' className='h-4 w-4' />
                Undo
              </button>
              
              <button type='button' 
                disabled={isSaving || Object.keys(counts).length === 0}
                onClick={resetCounts}
                className='flex items-center gap-1 bg-red-100 rounded-lg text-orange-700 text-sm 
                           disabled:opacity-50 disabled:cursor-not-allowed px-3 py-2'
              >
                <Trash aria-hidden='true' className='h-4 w-4' />
                Reset
              </button>
            </div>
          </div>

          {Object.keys(counts).length === 0 ? (
            <p className='flex flex-1 items-center justify-center text-center text-slate-400'>No items scanned yet</p>
          ) : (
            <div className='mt-4 min-h-0 flex-1 overflow-y-auto'>
              {Object.entries(counts).map(([productBarcode, quantity]) => {
                return (
                  <div key={productBarcode} className='mt-2 flex items-center justify-between rounded-lg bg-slate-50 p-3 gap-6'>
                    <p className='text-sm'>{products[productBarcode]}</p>
                    <p className='text-xl font-bold text-indigo-600'>{quantity}</p>
                  </div>
                );
              })}
            </div>
          )}
        </section>


        {/* UNRECOGNIZED BARCODE POPUP */}
        {unknownBarcode !== null && (
          <div className='fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4'>
            <div
              role='alertdialog'
              className='w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl'
            >
              <h2 id='unknown-barcode-title' className='text-xl font-bold text-red-800'>
                Barcode not recognized
              </h2> 

              <p id='unknown-barcode-message' className='mt-3  text-slate-600'>
                Unknown barcode: {unknownBarcode}
              </p>

              <p id='unknown-barcode-advice' className='mt-1  text-slate-600'>
                Check that you selected the correct location
              </p>

              <button
                type='button'
                onClick={() => setUnknownBarcode(null)}
                className='mt-6 w-full rounded-lg bg-indigo-600 py-3 font-semibold text-white'
              >
                Continue Scanning
              </button>
            </div>
          </div>
        )}


        {/* SAVE SUCCESS NOTICE */}
        {saveSuccess !== '' && (
          <p role='status' className='mt-3 shrink-0 text-center text-sm text-green-700'>{saveSuccess}</p>
        )}

        {saveError !== '' && (
          <p role='alert' className='mt-3 shrink-0 whitespace-pre-line text-center text-sm text-red-600'>{saveError}</p>
        )}


        {/* SAVE TO GOOGLE DRIVE BUTTON */}
        <button
          type='button'
          disabled={isSaving || Object.keys(counts).length === 0}
          onClick={saveReturns}
          className='w-full rounded-lg bg-indigo-600 text-lg font-semibold text-white shadow-md mt-4 shrink-0 py-4
                    disabled:bg-gray-300 disabled:cursor-not-allowed'
        >
          Save to Google Drive
        </button>

      </div>
    </main>
  )
}

export default App

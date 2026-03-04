async function run() {
  try {
    const pRes = await fetch('https://data.moa.gov.tw/api/v1/AnimalTransData/?$top=10');
    const pData = await pRes.json();
    console.log("Animal:", pData.Data.slice(0, 2));
  } catch(e) { console.log(e); }
}

run();

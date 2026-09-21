const fs=require('node:fs'),http=require('node:http'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require('playwright');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/index.html'));
const server=http.createServer((req,res)=>{res.setHeader('Content-Type','text/html');res.end(html)});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const browser=await chromium.launch({headless:true,args:['--no-sandbox']});
 const context=await browser.newContext({viewport:{width:360,height:780},timezoneId:'Asia/Kolkata'});
 const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
 try{
 await page.goto('http://127.0.0.1:'+server.address().port);
 assert.equal(await page.evaluate(()=>typeof window.startTelegramBackup),'function');
 await page.locator('#addBtn').click();
 await page.locator('#personName').fill('Demo Customer');await page.locator('#phone').fill('9876543210');
 await page.locator('#service').selectOption('Netflix');await page.locator('#loginId').fill('demo@example.com');await page.locator('#loginPassword').fill('Private!123');
 assert.equal(await page.locator('#loginPassword').getAttribute('type'),'password');
 await page.locator('#togglePassword').click();assert.equal(await page.locator('#loginPassword').getAttribute('type'),'text');
 await page.locator('#editForm button[type=submit]').click();
 await page.locator('.add-service').click();
 assert.equal(await page.locator('#personName').inputValue(),'Demo Customer');
 assert.equal(await page.locator('#phone').inputValue(),'+919876543210');
 assert.equal(await page.locator('#loginPassword').inputValue(),'');
 await page.locator('#service').selectOption('Other');assert.equal(await page.locator('#customService').isVisible(),true);
 await page.locator('#customService').fill('My Other Service');await page.locator('#editForm button[type=submit]').click();
 assert.equal(await page.locator('.card').count(),2);
 assert.ok((await page.locator('#resultCount').innerText()).includes('1 people'));
 await page.reload();assert.equal(await page.locator('.card').count(),2);
 await page.getByRole('button',{name:'Edit / Login',exact:true}).first().click();
 assert.equal(await page.locator('#loginPassword').getAttribute('type'),'password');
 assert.equal(await page.locator('#loginPassword').inputValue(),'Private!123');
 await page.locator('#editModal [data-close]').click();
 const result=await page.evaluate(async()=>{
   const plain=JSON.stringify(data),password='Test Backup Password!';
   const encrypted=await encryptBackup(plain,password);
   const decrypted=await decryptBackup(JSON.parse(encrypted),password);
   let wrongRejected=false;try{await decryptBackup(JSON.parse(encrypted),'incorrect')}catch(e){wrongRejected=true}
   const tampered=JSON.parse(encrypted);let bytes=base64Bytes(tampered.cipher);bytes[5]^=1;tampered.cipher=bytesBase64(bytes);
   let tamperRejected=false;try{await decryptBackup(tampered,password)}catch(e){tamperRejected=true}
   return {same:decrypted===plain,wrongRejected,tamperRejected,leaks:encrypted.includes('Private!123')};
 });
 assert.deepEqual(result,{same:true,wrongRejected:true,tamperRejected:true,leaks:false});
 // A second ID for the SAME customer and SAME app must remain independent.
 await page.locator('.add-service').first().click();
 await page.locator('#service').selectOption('Netflix');await page.locator('#accountLabel').fill('Netflix ID 2');
 await page.locator('#amount').fill('300');await page.locator('#initialPaid').check();
 await page.locator('#editForm button[type=submit]').click();
 assert.equal(await page.locator('.card').count(),3);
 await page.evaluate(()=>{const r=data.subscriptions.find(x=>x.accountLabel==='Netflix ID 2');const start=new Date(localDate(Date.now()-25*DAY)).getTime();persist({...data,subscriptions:data.subscriptions.map(x=>x.id===r.id?{...x,start,expiry:start+30*DAY}:x)});render()});
 const secondId=page.locator('.card').filter({has:page.locator('.account-label',{hasText:'Netflix ID 2'})});
 await secondId.getByRole('button',{name:'Stop / Refund',exact:true}).click();
 await page.locator('#refundBasis').fill('300');await page.locator('#useSuggested').click();
 assert.equal(await page.locator('#refundAmount').inputValue(),'50.00');
 await page.locator('#refundForm button[type=submit]').click();
 assert.equal(await page.locator('.card.stopped').count(),1);
 assert.equal(await page.locator('#activeCount').innerText(),'2');
 await page.locator('#incomeBtn').click();
 assert.ok((await page.locator('#incomeSummary').innerText()).includes('₹300.00'));
 await page.locator('#expenseBtn').click();await page.locator('#expenseAmount').fill('200');await page.locator('#expenseNote').fill('Supplier purchase');
 await page.locator('#expenseForm button[type=submit]').click();
 assert.equal(await page.locator('.income-total').innerText(),'₹100.00');
 page.once('dialog',d=>d.accept());await page.getByRole('button',{name:'Mark paid today',exact:true}).click();
 assert.equal(await page.locator('.income-total').innerText(),'₹50.00');
 fs.mkdirSync('output',{recursive:true});await page.screenshot({path:'output/my-income-360.png'});
 await page.locator('#incomeModal [data-close]').click();await page.reload();
 assert.equal(await page.locator('.card.stopped').count(),1);
 const ledger=await page.evaluate(async()=>validate(JSON.parse(await decryptBackup(JSON.parse(await encryptBackup(JSON.stringify(data),'Test Backup Password!')),'Test Backup Password!'))));
 assert.equal(ledger.refunds[0].status,'paid');assert.equal(ledger.expenses[0].amountPaise,20000);assert.equal(ledger.payments[0].accountLabel,'Netflix ID 2');
 await page.locator('#calculatorBtn').click();assert.ok((await page.locator('#calcResult').innerText()).includes('₹50.00'));
 await page.locator('#calcDays').fill('0');assert.ok((await page.locator('#calcResult').innerText()).includes('Enter a valid'));
 await page.locator('#calculatorModal [data-close]').click();
 // Search IDs without exposing passwords, then check new dialogs at narrow sizes.
 await page.locator('#search').fill('Netflix ID 2');assert.equal(await page.locator('.card').count(),1);
 await page.locator('#search').fill('');
 for(const width of [320,360,412]){
   await page.setViewportSize({width,height:640});
   for(const [button,modal] of [['incomeBtn','incomeModal'],['calculatorBtn','calculatorModal']]){
     await page.locator('#'+button).click();
     assert.equal(await page.locator('#'+modal+' .panel').evaluate(e=>e.scrollWidth<=e.clientWidth),true,'New dialog fits '+width);
     await page.locator('#'+modal+' [data-close]').click();
   }
 }
 await page.setViewportSize({width:360,height:780});

 const EPS=1;
 for(const width of [320,360,412]){
   await page.setViewportSize({width,height:780});
   await page.evaluate(()=>scrollTo(0,0));
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true,'No horizontal overflow at '+width);
   const header=await page.locator('header').boundingBox();
   assert.ok(header&&header.y>=-EPS&&header.x>=-EPS&&header.x+header.width<=width+EPS,'Header stays inside viewport at '+width);
   await page.locator('#addBtn').click();
   const panel=await page.locator('#editModal .panel').boundingBox();assert.ok(panel&&panel.y>=-EPS&&panel.y+panel.height<=780+EPS);
   assert.equal(await page.locator('#editModal .panel').evaluate(e=>e.scrollWidth<=e.clientWidth),true,'No form overflow at '+width);
   await page.locator('#editModal [data-close]').click();
 }
 await page.setViewportSize({width:360,height:780});
 fs.mkdirSync('output',{recursive:true});
 await page.screenshot({path:'output/dashboard-360.png',fullPage:true});
 await page.getByRole('button',{name:'Edit / Login',exact:true}).first().click();
 await page.locator('#loginId').scrollIntoViewIfNeeded();
 await page.screenshot({path:'output/subscription-form-360.png'});
 assert.deepEqual(errors,[]);
 console.log('PASS: mobile overflow checks, multiple subscriptions per customer, custom service, masked credential persistence, encrypted backup round-trip, wrong-password/tamper rejection, same-app multiple IDs, 30/25 refund, expenses, income, paid status persistence, ID search and new dialog alignment.');
 }finally{await browser.close();await new Promise(resolve=>server.close(resolve))}
})().catch(e=>{console.error(e);server.close();process.exitCode=1});


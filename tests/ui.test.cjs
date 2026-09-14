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
 console.log('PASS: mobile overflow checks, multiple subscriptions per customer, custom service, masked credential persistence, encrypted backup round-trip, wrong-password/tamper rejection.');
 }finally{await browser.close();await new Promise(resolve=>server.close(resolve))}
})().catch(e=>{console.error(e);server.close();process.exitCode=1});

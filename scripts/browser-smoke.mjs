import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const screenshotDir = path.join(repoRoot, 'docs', 'screenshots')
const baseUrl = process.env.FRONTEND_URL || 'http://127.0.0.1:5173'
const apiUrl = process.env.API_URL || 'http://127.0.0.1:8080/api'
const chromePath =
  process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const puppeteerModule =
  process.env.PUPPETEER_MODULE ||
  'file:///C:/Users/Chengzi/.codex/tools/mermaid-cli/node_modules/puppeteer/lib/puppeteer/puppeteer.js'

const { default: puppeteer } = await import(
  puppeteerModule.startsWith('file:')
    ? puppeteerModule
    : pathToFileURL(path.resolve(puppeteerModule)).href
)

await fs.mkdir(screenshotDir, { recursive: true })

const browser = await puppeteer.launch({
  headless: true,
  executablePath: chromePath,
  args: ['--no-sandbox', '--disable-dev-shm-usage']
})
const page = await browser.newPage()
await page.setViewport({ width: 1440, height: 1000, deviceScaleFactor: 1 })

const issues = []
const warnings = []
const results = []
const createdIds = []
const uniqueSuffix = Date.now().toString(36)

page.on('pageerror', (error) => {
  issues.push({ type: 'pageerror', url: page.url(), message: error.message })
})
page.on('console', (message) => {
  if (message.type() === 'error' && !message.text().includes('favicon')) {
    const locationUrl = message.location()?.url || ''
    const externalAsset404 =
      message.text().includes('404') &&
      locationUrl &&
      !locationUrl.startsWith(baseUrl) &&
      !locationUrl.startsWith(apiUrl)
    const entry = { type: 'console', url: locationUrl || page.url(), message: message.text() }
    if (externalAsset404) {
      warnings.push(entry)
    } else {
      issues.push(entry)
    }
  }
})
page.on('response', async (response) => {
  const responseUrl = response.url()
  if (responseUrl.includes('/api/') && response.status() >= 400) {
    issues.push({
      type: 'http',
      url: page.url(),
      message: `${response.status()} ${response.request().method()} ${responseUrl}`
    })
  } else if (
    response.request().resourceType() === 'image' &&
    response.status() >= 400 &&
    !responseUrl.startsWith(baseUrl)
  ) {
    warnings.push({
      type: 'external-image',
      url: page.url(),
      message: `${response.status()} ${responseUrl}`
    })
  }
})

const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds))

async function waitForNetwork() {
  await page.waitForNetworkIdle({ idleTime: 350, timeout: 15000 }).catch(() => undefined)
}

async function clickButton(text, scope = 'body') {
  const clicked = await page.evaluate(
    ({ buttonText, rootSelector }) => {
      const root = document.querySelector(rootSelector)
      const button = root
        ? [...root.querySelectorAll('button')].find(
            (candidate) => candidate.innerText.trim() === buttonText
          )
        : undefined
      button?.click()
      return Boolean(button)
    },
    { buttonText: text, rootSelector: scope }
  )
  if (!clicked) throw new Error(`Button not found: ${text}`)
}

async function clickElementByText(selector, text, exact = true) {
  const clicked = await page.evaluate(
    ({ elementSelector, expectedText, requireExact }) => {
      const candidates = [...document.querySelectorAll(elementSelector)]
      const element = candidates.find((candidate) => {
        const candidateText = candidate.textContent?.trim() || ''
        return requireExact
          ? candidateText === expectedText
          : candidateText.includes(expectedText)
      })
      element?.click()
      return Boolean(element)
    },
    { elementSelector: selector, expectedText: text, requireExact: exact }
  )
  if (!clicked) throw new Error(`Element not found: ${selector} / ${text}`)
}

async function clickRowButton(rowText, buttonText) {
  await page.waitForFunction(
    (value) => [...document.querySelectorAll('tbody tr')].some((row) => row.innerText.includes(value)),
    { timeout: 10000 },
    rowText
  )
  const clicked = await page.evaluate(
    ({ value, action }) => {
      const row = [...document.querySelectorAll('tbody tr')].find((candidate) =>
        candidate.innerText.includes(value)
      )
      const button = row
        ? [...row.querySelectorAll('button')].find(
            (candidate) => candidate.innerText.trim() === action
          )
        : undefined
      button?.click()
      return Boolean(button)
    },
    { value: rowText, action: buttonText }
  )
  if (!clicked) throw new Error(`Row action not found: ${rowText} / ${buttonText}`)
}

async function setField(label, value) {
  await page.waitForSelector('.el-dialog', { visible: true })
  const outcome = await page.evaluate(
    ({ fieldLabel, fieldValue }) => {
      const items = [...document.querySelectorAll('.el-dialog .el-form-item')]
      const item = items.find(
        (candidate) =>
          candidate.querySelector('.el-form-item__label')?.textContent.trim() === fieldLabel
      )
      const input = item?.querySelector('textarea, input')
      if (!(input instanceof HTMLInputElement || input instanceof HTMLTextAreaElement)) {
        return { ok: false, labels: items.map((entry) => entry.innerText.split('\n')[0]) }
      }
      const prototype =
        input instanceof HTMLTextAreaElement
          ? HTMLTextAreaElement.prototype
          : HTMLInputElement.prototype
      const setter = Object.getOwnPropertyDescriptor(prototype, 'value')?.set
      setter?.call(input, String(fieldValue))
      input.dispatchEvent(new Event('input', { bubbles: true }))
      input.dispatchEvent(new Event('change', { bubbles: true }))
      input.dispatchEvent(new FocusEvent('blur', { bubbles: true }))
      return { ok: true, value: input.value, type: input.type }
    },
    { fieldLabel: label, fieldValue: value }
  )
  if (!outcome.ok) {
    throw new Error(`Form field not found: ${label}; available=${outcome.labels?.join(', ')}`)
  }
}

async function searchRows(keyword) {
  await page.waitForSelector('[data-testid="crud-search"]')
  await page.evaluate(
    ({ selector, value }) => {
      const element = document.querySelector(selector)
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set
      setter?.call(element, value)
      element.dispatchEvent(new Event('input', { bubbles: true }))
      element.dispatchEvent(new Event('change', { bubbles: true }))
    },
    { selector: '[data-testid="crud-search"]', value: keyword }
  )
  await page.click('[data-testid="crud-search-submit"]')
  await waitForNetwork()
}

async function waitForResponse(method, pathFragment) {
  return page.waitForResponse(
    (response) =>
      response.request().method() === method && response.url().includes(pathFragment),
    { timeout: 20000 }
  )
}

async function assertResponseOk(response, context) {
  if (response.status() >= 400) {
    const body = await response.text()
    throw new Error(`${context}: HTTP ${response.status()} ${body.slice(0, 500)}`)
  }
}

async function login(username, password, destination) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle0', timeout: 30000 })
  await page.type('input[autocomplete="username"]', username)
  await page.type('input[autocomplete="current-password"]', password)
  await clickButton('登录')
  await page.waitForFunction(
    (expectedPath) => {
      if (expectedPath) return window.location.pathname.startsWith(expectedPath)
      return !window.location.pathname.startsWith('/login')
    },
    { timeout: 15000 },
    destination
  )
  await waitForNetwork()
}

async function apiRequest(method, pathname, token, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const response = await fetch(`${apiUrl}${pathname}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  if (!response.ok) {
    throw new Error(`${method} ${pathname}: HTTP ${response.status()} ${await response.text()}`)
  }
  return response.status === 204 ? null : response.json()
}

async function apiLogin(username, password) {
  const response = await apiRequest('POST', '/auth/login', '', { username, password })
  return response.data.token
}

async function setInputByPlaceholder(placeholder, value) {
  await page.waitForSelector(`input[placeholder="${placeholder}"]`, { visible: true })
  await page.evaluate(
    ({ selector, fieldValue }) => {
      const input = document.querySelector(selector)
      if (!(input instanceof HTMLInputElement)) {
        throw new Error(`Input not found: ${selector}`)
      }
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set
      setter?.call(input, String(fieldValue))
      input.dispatchEvent(new Event('input', { bubbles: true }))
      input.dispatchEvent(new Event('change', { bubbles: true }))
    },
    { selector: `input[placeholder="${placeholder}"]`, fieldValue: value }
  )
}

async function selectFilterOption(index, label) {
  await page.evaluate((selectIndex) => {
    const select = document.querySelectorAll('.filter-bar .el-select')[selectIndex]
    select?.click()
  }, index)
  await wait(250)
  const clicked = await page.evaluate((optionLabel) => {
    const items = [...document.querySelectorAll('.el-select-dropdown__item')]
    const item = items.find(
      (candidate) =>
        candidate.textContent?.trim() === optionLabel &&
        candidate.getBoundingClientRect().height > 0
    )
    item?.click()
    return Boolean(item)
  }, label)
  if (!clicked) throw new Error(`Select option not found: ${label}`)
}

async function setVisibleLabNumberInput(value) {
  const updated = await page.evaluate((nextValue) => {
    const pane = [...document.querySelectorAll('.el-tab-pane')].find(
      (candidate) => candidate.getBoundingClientRect().height > 0
    )
    const input = pane?.querySelector('.el-input-number input')
    if (!(input instanceof HTMLInputElement)) return false
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set
    setter?.call(input, String(nextValue))
    input.dispatchEvent(new Event('input', { bubbles: true }))
    input.dispatchEvent(new Event('change', { bubbles: true }))
    input.dispatchEvent(new FocusEvent('blur', { bubbles: true }))
    return true
  }, value)
  if (!updated) throw new Error('Visible database lab number input was not found')
}

async function runCheck(name, action) {
  try {
    const evidence = await action()
    results.push({ name, status: 'PASS', evidence })
  } catch (error) {
    results.push({
      name,
      status: 'FAIL',
      message: error instanceof Error ? error.message : String(error)
    })
    issues.push({
      type: 'test',
      url: page.url(),
      message: `${name}: ${error instanceof Error ? error.message : String(error)}`
    })
    throw error
  }
}

async function crudScenario({
  name,
  route,
  endpoint,
  createFields,
  updateFields,
  searchBefore,
  searchAfter
}) {
  let createdId
  try {
    await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle0', timeout: 30000 })
    await page.waitForSelector('tbody tr', { timeout: 15000 })
    await page.click('[data-testid="crud-create"]')
    await page.waitForSelector('.el-dialog', { visible: true })
    for (const [label, value] of Object.entries(createFields)) await setField(label, value)

    const createdResponsePromise = waitForResponse('POST', endpoint)
    await page.click('[data-testid="crud-save"]')
    const createdResponse = await createdResponsePromise
    await assertResponseOk(createdResponse, `${name} create`)
    const createdEnvelope = await createdResponse.json()
    createdId = createdEnvelope.data?.id
    if (createdId === undefined) throw new Error(`${name} create response has no id`)
    createdIds.push({ endpoint, id: createdId })
    await page.waitForSelector('.el-dialog', { hidden: true, timeout: 10000 })

    await searchRows(searchBefore)
    await page.waitForFunction(
      (value) => document.body.innerText.includes(value),
      { timeout: 10000 },
      searchBefore
    )

    await clickRowButton(searchBefore, '编辑')
    for (const [label, value] of Object.entries(updateFields)) await setField(label, value)
    const updatedResponsePromise = waitForResponse('PUT', `${endpoint}/${createdId}`)
    await page.click('[data-testid="crud-save"]')
    await assertResponseOk(await updatedResponsePromise, `${name} update`)
    await page.waitForSelector('.el-dialog', { hidden: true, timeout: 10000 })

    await searchRows(searchAfter)
    await page.waitForFunction(
      (value) => document.body.innerText.includes(value),
      { timeout: 10000 },
      searchAfter
    )

    await clickRowButton(searchAfter, '删除')
    await page.waitForSelector('.el-message-box', { visible: true, timeout: 10000 })
    const deletedResponsePromise = waitForResponse('DELETE', `${endpoint}/${createdId}`)
    await clickButton('确认删除', '.el-message-box')
    await assertResponseOk(await deletedResponsePromise, `${name} delete`)
    createdIds.splice(
      createdIds.findIndex((entry) => entry.id === createdId && entry.endpoint === endpoint),
      1
    )
    results.push({ name: `${name} UI CRUD`, status: 'PASS', id: createdId })
  } catch (error) {
    results.push({
      name: `${name} UI CRUD`,
      status: 'FAIL',
      id: createdId,
      message: error instanceof Error ? error.message : String(error)
    })
    throw error
  }
}

async function screenshot(route, filename, waitSelector) {
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle0', timeout: 30000 })
  if (waitSelector) await page.waitForSelector(waitSelector, { timeout: 20000 })
  await wait(600)
  await page.evaluate(async () => {
    const step = Math.max(Math.round(window.innerHeight * 0.8), 400)
    for (let offset = 0; offset < document.documentElement.scrollHeight; offset += step) {
      window.scrollTo(0, offset)
      await new Promise((resolve) => setTimeout(resolve, 120))
    }
    window.scrollTo(0, 0)
  })
  await wait(700)
  await page.screenshot({ path: path.join(screenshotDir, filename), fullPage: true })
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1
  )
  if (overflow) issues.push({ type: 'layout', url: page.url(), message: 'Horizontal overflow' })
  results.push({ name: `Screenshot ${filename}`, status: 'PASS', route })
}

let token
let labTriggerAnimeId
try {
  await runCheck('Registration form blocks empty and 3-character usernames', async () => {
    await page.goto(`${baseUrl}/register`, { waitUntil: 'networkidle0', timeout: 30000 })
    await page.evaluate(() => localStorage.clear())
    await page.reload({ waitUntil: 'networkidle0', timeout: 30000 })
    await page.waitForSelector('input[placeholder="字母、数字或下划线"]', { visible: true })
    await clickButton('创建账号')
    await page.waitForFunction(
      () => document.body.innerText.includes('请输入用户名'),
      { timeout: 5000 }
    )

    await setInputByPlaceholder('字母、数字或下划线', 'abc')
    await setInputByPlaceholder('展示名称', '短名测试')
    await setInputByPlaceholder('name@example.com', 'short-ui@animeverse.local')
    await setInputByPlaceholder('至少 8 个字符', 'Course123!')
    await setInputByPlaceholder('再次输入密码', 'Course123!')
    await clickButton('创建账号')
    await page.waitForFunction(
      () => document.body.innerText.includes('用户名长度应为 4 至 32 个字符'),
      { timeout: 5000 }
    )
    if (new URL(page.url()).pathname !== '/register') {
      throw new Error(`Registration unexpectedly navigated to ${page.url()}`)
    }
    return 'empty and 3-character username rejected client-side'
  })

  await login('admin', 'Admin123!', '/admin')
  token = await page.evaluate(() => localStorage.getItem('animeverse_token'))
  if (!token) throw new Error('Admin token was not stored after UI login')

  await crudScenario({
    name: 'Genre',
    route: '/admin/genres',
    endpoint: '/admin/genres',
    createFields: {
      类型名称: `UI类型${uniqueSuffix}`,
      类型说明: '浏览器自动化临时类型'
    },
    updateFields: {
      类型名称: `UI类型${uniqueSuffix}改`,
      类型说明: '浏览器自动化已更新'
    },
    searchBefore: `UI类型${uniqueSuffix}`,
    searchAfter: `UI类型${uniqueSuffix}改`
  })

  await crudScenario({
    name: 'Studio',
    route: '/admin/studios',
    endpoint: '/admin/studios',
    createFields: {
      厂商名称: `UI Studio ${uniqueSuffix}`,
      '国家/地区': '日本',
      厂商简介: '浏览器自动化临时厂商'
    },
    updateFields: {
      厂商名称: `UI Studio ${uniqueSuffix} Updated`,
      厂商简介: '浏览器自动化已更新'
    },
    searchBefore: `UI Studio ${uniqueSuffix}`,
    searchAfter: `UI Studio ${uniqueSuffix} Updated`
  })

  await crudScenario({
    name: 'Platform',
    route: '/admin/platforms',
    endpoint: '/admin/platforms',
    createFields: {
      平台名称: `UI Platform ${uniqueSuffix}`,
      平台编码: `ui-platform-${uniqueSuffix}`,
      '官网 URL': `https://example.com/${uniqueSuffix}`,
      地区: '中国'
    },
    updateFields: {
      平台名称: `UI Platform ${uniqueSuffix} Updated`,
      地区: '日本'
    },
    searchBefore: `UI Platform ${uniqueSuffix}`,
    searchAfter: `UI Platform ${uniqueSuffix} Updated`
  })

  await crudScenario({
    name: 'Staff',
    route: '/admin/staff',
    endpoint: '/admin/staff',
    createFields: {
      姓名: `UI Staff ${uniqueSuffix}`,
      原名: `UI Staff JP ${uniqueSuffix}`,
      '国家/地区': '日本',
      人物简介: '浏览器自动化临时人员'
    },
    updateFields: {
      姓名: `UI Staff ${uniqueSuffix} Updated`,
      人物简介: '浏览器自动化已更新'
    },
    searchBefore: `UI Staff ${uniqueSuffix}`,
    searchAfter: `UI Staff ${uniqueSuffix} Updated`
  })

  await crudScenario({
    name: 'Episode',
    route: '/admin/episodes',
    endpoint: '/admin/episodes',
    createFields: {
      '所属动漫 ID': 1,
      集数: 999,
      剧集标题: `UI Episode ${uniqueSuffix}`,
      '时长（秒）': 1500,
      剧集简介: '浏览器自动化临时剧集'
    },
    updateFields: {
      剧集标题: `UI Episode ${uniqueSuffix} Updated`,
      剧集简介: '浏览器自动化已更新'
    },
    searchBefore: `UI Episode ${uniqueSuffix}`,
    searchAfter: `UI Episode ${uniqueSuffix} Updated`
  })

  await crudScenario({
    name: 'Anime',
    route: '/admin/anime',
    endpoint: '/admin/anime',
    createFields: {
      动漫名称: `UI Anime ${uniqueSuffix}`,
      原名: `UI Anime JP ${uniqueSuffix}`,
      '制作厂商 ID': 1,
      首播年份: 2026,
      简介: '浏览器自动化临时动漫'
    },
    updateFields: {
      动漫名称: `UI Anime ${uniqueSuffix} Updated`,
      简介: '浏览器自动化已更新'
    },
    searchBefore: `UI Anime ${uniqueSuffix}`,
    searchAfter: `UI Anime ${uniqueSuffix} Updated`
  })

  await crudScenario({
    name: 'Role',
    route: '/admin/roles',
    endpoint: '/admin/roles',
    createFields: {
      角色名称: `UI角色${uniqueSuffix}`,
      角色编码: `UIROLE${uniqueSuffix.toUpperCase()}`,
      角色说明: '浏览器自动化临时角色'
    },
    updateFields: {
      角色名称: `UI角色${uniqueSuffix}改`,
      角色说明: '浏览器自动化已更新'
    },
    searchBefore: `UI角色${uniqueSuffix}`,
    searchAfter: `UI角色${uniqueSuffix}改`
  })

  await runCheck('Statistics cards and charts render from API data', async () => {
    await page.goto(`${baseUrl}/admin/statistics`, {
      waitUntil: 'networkidle0',
      timeout: 30000
    })
    await page.waitForSelector('.statistics-card', { timeout: 15000 })
    await page.waitForSelector('canvas', { timeout: 15000 })
    await wait(500)
    const cardCount = await page.$$eval('.statistics-card', (elements) => elements.length)
    const chartEvidence = await page.$$eval('canvas', (canvases) =>
      canvases
        .filter((canvas) => canvas.getBoundingClientRect().height > 0)
        .map((canvas) => ({
          width: canvas.width,
          height: canvas.height,
          pixels: canvas
            .getContext('2d')
            ?.getImageData(0, 0, canvas.width, canvas.height)
            .data.some((value, index) => index % 4 === 3 && value > 0)
        }))
    )
    if (cardCount !== 6) throw new Error(`Expected 6 statistics cards, got ${cardCount}`)
    if (chartEvidence.length < 2 || chartEvidence.some((chart) => !chart.pixels)) {
      throw new Error(`Statistics charts are blank: ${JSON.stringify(chartEvidence)}`)
    }
    return `cards=${cardCount}; canvases=${chartEvidence.length}; nonblank=true`
  })

  await runCheck('User role restoration and unchanged save', async () => {
    const rolesResponse = await apiRequest('GET', '/admin/roles', token)
    const userRole = rolesResponse.data.find((role) => role.code === 'USER')
    if (!userRole) throw new Error('USER role was not returned by /admin/roles')

    await page.goto(`${baseUrl}/admin/users`, {
      waitUntil: 'networkidle0',
      timeout: 30000
    })
    await setInputByPlaceholder('搜索用户名、昵称或邮箱', 'sakura')
    await clickButton('搜索', '.admin-toolbar')
    await waitForNetwork()
    await clickRowButton('sakura', '分配角色')
    await page.waitForSelector('.el-dialog', { visible: true })
    const selectedText = await page.$eval('.el-dialog .el-select', (element) =>
      element.textContent.trim()
    )
    const expectedRoleName = userRole.name || userRole.roleName || userRole.code
    if (!selectedText.includes(expectedRoleName)) {
      throw new Error(
        `Existing USER role was not restored; selected="${selectedText}", expected="${expectedRoleName}"`
      )
    }

    const savedResponsePromise = waitForResponse('PUT', '/admin/users/')
    await clickButton('保存', '.el-dialog')
    await assertResponseOk(await savedResponsePromise, 'Unchanged user role save')
    await page.waitForSelector('.el-dialog', { hidden: true, timeout: 10000 })
    return `selected=${expectedRoleName}`
  })

  await runCheck('Anime score column uses averageRating and aggregate fields stay readonly', async () => {
    const detail = await apiRequest('GET', '/anime/8', '')
    const expectedRating = Number(detail.data.averageRating || detail.data.rating || 0).toFixed(1)
    await page.goto(`${baseUrl}/admin/anime`, {
      waitUntil: 'networkidle0',
      timeout: 30000
    })
    await searchRows('葬送的芙莉莲')
    await page.waitForFunction(
      (rating) => {
        const row = [...document.querySelectorAll('tbody tr')].find((candidate) =>
          candidate.innerText.includes('葬送的芙莉莲')
        )
        return row?.innerText.includes(rating)
      },
      { timeout: 10000 },
      expectedRating
    )
    await clickRowButton('葬送的芙莉莲', '编辑')
    await page.waitForSelector('.el-dialog', { visible: true })
    const labels = await page.$$eval('.el-dialog .el-form-item__label', (elements) =>
      elements.map((element) => element.textContent.trim())
    )
    if (labels.some((label) => label === '评分' || label === '集数')) {
      throw new Error(`Aggregate fields are editable: ${labels.join(', ')}`)
    }
    await clickButton('取消', '.el-dialog')
    return `rating=${expectedRating}; editableAggregates=none`
  })

  await runCheck('Hidden review can be restored and deleted', async () => {
    const userTokenForReview = await apiLogin('sakura', 'User123!')
    const mine = await apiRequest('GET', '/user/reviews?page=1&size=100', userTokenForReview)
    const reviewedIds = new Set(
      (mine.data.records || []).map((review) => Number(review.animeId))
    )
    const animeId = Array.from({ length: 30 }, (_, index) => index + 1).find(
      (candidate) => !reviewedIds.has(candidate)
    )
    if (!animeId) throw new Error('No unreviewed anime available for review lifecycle test')
    const content = `UI审核临时${uniqueSuffix}`
    const created = await apiRequest(
      'POST',
      `/anime/${animeId}/reviews`,
      userTokenForReview,
      { rating: 8, content }
    )
    const reviewId = created.data.id

    try {
      await page.goto(`${baseUrl}/admin/reviews`, {
        waitUntil: 'networkidle0',
        timeout: 30000
      })
      await setInputByPlaceholder('搜索评论内容、用户或动漫', content)
      await clickButton('搜索', '.admin-toolbar')
      await waitForNetwork()
      await page.waitForFunction(
        (needle) => document.body.innerText.includes(needle),
        { timeout: 10000 },
        content
      )
      let responsePromise = waitForResponse('PUT', `/admin/reviews/${reviewId}/visibility`)
      await clickRowButton(content, '隐藏')
      await assertResponseOk(await responsePromise, 'Hide review')
      await page.waitForFunction(
        (needle) => {
          const row = [...document.querySelectorAll('tbody tr')].find((candidate) =>
            candidate.innerText.includes(needle)
          )
          return row?.innerText.includes('已隐藏') && row?.innerText.includes('恢复显示')
        },
        { timeout: 10000 },
        content
      )

      responsePromise = waitForResponse('PUT', `/admin/reviews/${reviewId}/visibility`)
      await clickRowButton(content, '恢复显示')
      await assertResponseOk(await responsePromise, 'Restore review')
      await page.waitForFunction(
        (needle) => {
          const row = [...document.querySelectorAll('tbody tr')].find((candidate) =>
            candidate.innerText.includes(needle)
          )
          return row?.innerText.includes('显示中') && row?.innerText.includes('隐藏')
        },
        { timeout: 10000 },
        content
      )

      const deletedResponsePromise = waitForResponse('DELETE', `/admin/reviews/${reviewId}`)
      await clickRowButton(content, '删除')
      await page.waitForSelector('.el-message-box', { visible: true, timeout: 10000 })
      await clickButton('确认删除', '.el-message-box')
      await assertResponseOk(await deletedResponsePromise, 'Delete review')
      return `reviewId=${reviewId}; visible/hidden/visible/delete`
    } catch (error) {
      await apiRequest('DELETE', `/admin/reviews/${reviewId}`, token).catch(() => undefined)
      throw error
    }
  })

  await page.evaluate(() => localStorage.clear())
  await login('sakura', 'User123!', '/')

  await runCheck('User pages, pagination, sorting and admin guard work', async () => {
    await page.goto(`${baseUrl}/`, { waitUntil: 'networkidle0', timeout: 30000 })
    await page.waitForSelector('.anime-card', { timeout: 15000 })
    const homeCards = await page.$$eval('.anime-card', (elements) => elements.length)
    const rankingItems = await page.$$eval('.ranking-mini', (elements) => elements.length)
    if (homeCards < 1 || rankingItems < 1) {
      throw new Error(`Home content missing: cards=${homeCards}, ranking=${rankingItems}`)
    }

    await page.goto(`${baseUrl}/anime?page=1`, { waitUntil: 'networkidle0', timeout: 30000 })
    await page.waitForSelector('.anime-card__title', { timeout: 15000 })
    const firstPageTitle = await page.$eval('.anime-card__title', (element) =>
      element.textContent.trim()
    )
    const pageTwoResponse = waitForResponse('GET', '/anime?page=2')
    await clickElementByText('.el-pager li', '2')
    await assertResponseOk(await pageTwoResponse, 'Anime page 2')
    await waitForNetwork()
    const secondPageTitle = await page.$eval('.anime-card__title', (element) =>
      element.textContent.trim()
    )
    if (firstPageTitle === secondPageTitle) {
      throw new Error('Pagination did not change the visible anime records')
    }

    await page.goto(`${baseUrl}/anime?page=1&sort=favorites`, {
      waitUntil: 'networkidle0',
      timeout: 30000
    })
    await page.waitForSelector('.anime-card__title', { timeout: 15000 })

    const sortOptions = [
      { label: '收藏最多', value: 'favorites' },
      { label: '评分最高', value: 'rating' },
      { label: '最新上线', value: 'latest' },
      { label: '标题排序', value: 'title' }
    ]
    const sortEvidence = []
    for (const option of sortOptions) {
      const expected = await apiRequest(
        'GET',
        `/anime?page=1&size=12&sort=${option.value}`,
        ''
      )
      const expectedTitle = expected.data.records[0]?.title
      let responsePromise
      if (option.value !== 'favorites') {
        responsePromise = waitForResponse('GET', `sort=${option.value}`)
        await selectFilterOption(2, option.label)
        await assertResponseOk(await responsePromise, `Anime sort ${option.value}`)
        await waitForNetwork()
      }
      const visibleTitle = await page.$eval('.anime-card__title', (element) =>
        element.textContent.trim()
      )
      if (!expectedTitle || visibleTitle !== expectedTitle) {
        throw new Error(
          `Sort ${option.value} mismatch: visible="${visibleTitle}", expected="${expectedTitle}"`
        )
      }
      sortEvidence.push(`${option.value}=${visibleTitle}`)
    }

    await page.goto(`${baseUrl}/anime/8`, { waitUntil: 'networkidle0', timeout: 30000 })
    await page.waitForFunction(
      () => document.querySelector('h1')?.textContent?.includes('葬送的芙莉莲'),
      { timeout: 15000 }
    )
    const episodeCount = await page.$$eval('.episode-item', (elements) => elements.length)
    if (episodeCount < 1) throw new Error('Anime detail has no episode controls')

    await page.goto(`${baseUrl}/studios`, { waitUntil: 'networkidle0', timeout: 30000 })
    await page.waitForSelector('.studio-card', { timeout: 15000 })
    const studioCount = await page.$$eval('.studio-card', (elements) => elements.length)
    if (studioCount !== 8) throw new Error(`Expected 8 studios, got ${studioCount}`)

    await page.goto(`${baseUrl}/ranking`, { waitUntil: 'networkidle0', timeout: 30000 })
    await page.waitForSelector('.podium-card', { timeout: 15000 })
    const podiumCount = await page.$$eval('.podium-card', (elements) => elements.length)
    if (podiumCount !== 3) throw new Error(`Expected 3 podium cards, got ${podiumCount}`)

    await page.goto(`${baseUrl}/admin`, { waitUntil: 'networkidle0', timeout: 30000 })
    await page.waitForFunction(
      () => !window.location.pathname.startsWith('/admin'),
      { timeout: 5000 }
    )
    return [
      `home=${homeCards}/${rankingItems}`,
      `pagination=${firstPageTitle}->${secondPageTitle}`,
      `sorts=${sortEvidence.join('|')}`,
      `episodes=${episodeCount}`,
      `studios=${studioCount}`,
      `podium=${podiumCount}`,
      'adminGuard=redirected'
    ].join('; ')
  })

  await runCheck('Favorite list state is correct on first click', async () => {
    const userTokenForFavorite = await apiLogin('sakura', 'User123!')
    const favoritesResponse = await apiRequest('GET', '/user/favorites', userTokenForFavorite)
    const favorite = favoritesResponse.data[0]
    if (!favorite) throw new Error('No favorite is available for first-click state test')
    const animeTitle = favorite.title || favorite.anime?.title
    const animeId = favorite.animeId || favorite.anime?.id
    if (!animeTitle || !animeId) throw new Error('Favorite payload lacks anime title/id')

    await page.goto(
      `${baseUrl}/anime?keyword=${encodeURIComponent(animeTitle)}`,
      { waitUntil: 'networkidle0', timeout: 30000 }
    )
    await page.waitForFunction(
      (title) =>
        [...document.querySelectorAll('.anime-card__title')].some(
          (element) => element.textContent.trim() === title
        ),
      { timeout: 15000 },
      animeTitle
    )
    const initiallyActive = await page.evaluate((title) => {
      const titleLink = [...document.querySelectorAll('.anime-card__title')].find(
        (element) => element.textContent.trim() === title
      )
      return titleLink
        ?.closest('.anime-card')
        ?.querySelector('.anime-card__favorite')
        ?.classList.contains('is-active')
    }, animeTitle)
    if (!initiallyActive) throw new Error('Favorited card did not render active on first load')

    const removeResponsePromise = waitForResponse('DELETE', `/user/favorites/${animeId}`)
    await page.evaluate((title) => {
      const titleLink = [...document.querySelectorAll('.anime-card__title')].find(
        (element) => element.textContent.trim() === title
      )
      titleLink?.closest('.anime-card')?.querySelector('.anime-card__favorite')?.click()
    }, animeTitle)
    await assertResponseOk(await removeResponsePromise, 'First-click favorite removal')
    await page.waitForFunction(
      (title) => {
        const titleLink = [...document.querySelectorAll('.anime-card__title')].find(
          (element) => element.textContent.trim() === title
        )
        const button = titleLink
          ?.closest('.anime-card')
          ?.querySelector('.anime-card__favorite')
        return button ? !button.classList.contains('is-active') : false
      },
      { timeout: 10000 },
      animeTitle
    )
    await apiRequest('POST', `/user/favorites/${animeId}`, userTokenForFavorite)
    return `animeId=${animeId}; first click removed favorite`
  })

  await page.evaluate(() => localStorage.clear())
  await page.setViewport({ width: 1440, height: 1000, deviceScaleFactor: 1 })
  await screenshot('/', '01-home.png', '.anime-card')
  await screenshot('/anime', '02-anime-list.png', '.anime-card')
  await screenshot('/anime/8', '03-anime-detail.png', 'h1')
  await screenshot('/ranking', '04-ranking.png', '.podium-card')

  await login('admin', 'Admin123!', '/admin')
  await screenshot('/admin', '05-admin-dashboard.png', '.admin-page-heading')
  await page.goto(`${baseUrl}/admin/lab`, { waitUntil: 'networkidle0', timeout: 30000 })
  await runCheck('Database lab executes all five real tabs', async () => {
    await page.waitForSelector('.lab-tabs', { timeout: 15000 })

    const viewResponsePromise = waitForResponse('GET', '/lab/views/v_anime_detail')
    await clickButton('执行查询')
    await assertResponseOk(await viewResponsePromise, 'Database lab view query')
    await page.waitForSelector('.lab-panel .el-table__row', { timeout: 15000 })
    const viewRows = await page.$$eval('.lab-panel .el-table__row', (elements) => elements.length)
    if (viewRows < 1) throw new Error('View query rendered no rows')

    const favorites = await apiRequest('GET', '/user/favorites', token)
    const adminFavoriteIds = new Set(
      (favorites.data || []).map((favorite) => Number(favorite.animeId))
    )
    const animeId = Array.from({ length: 30 }, (_, index) => index + 1).find(
      (candidate) => !adminFavoriteIds.has(candidate)
    )
    if (!animeId) throw new Error('No unfavorited anime available for trigger lab')

    await clickElementByText('.el-tabs__item', '触发器')
    await page.waitForFunction(
      () =>
        [...document.querySelectorAll('.el-tab-pane')].some(
          (element) =>
            element.getBoundingClientRect().height > 0 &&
            element.textContent?.includes('收藏计数触发器')
        ),
      { timeout: 5000 }
    )
    await setVisibleLabNumberInput(animeId)
    let responsePromise = waitForResponse('POST', '/lab/trigger/favorite')
    await clickButton('执行触发器')
    const enabledResponse = await responsePromise
    await assertResponseOk(enabledResponse, 'Trigger favorite enable')
    const enabled = await enabledResponse.json()
    if (enabled.data.afterCount !== enabled.data.beforeCount + 1) {
      throw new Error('Trigger insert did not increment favorite count')
    }
    labTriggerAnimeId = animeId

    await page.evaluate(() => {
      const pane = [...document.querySelectorAll('.el-tab-pane')].find(
        (candidate) => candidate.getBoundingClientRect().height > 0
      )
      pane?.querySelector('.el-switch')?.click()
    })
    await wait(200)
    responsePromise = waitForResponse('POST', '/lab/trigger/favorite')
    await clickButton('执行触发器')
    const disabledResponse = await responsePromise
    await assertResponseOk(disabledResponse, 'Trigger favorite disable')
    const disabled = await disabledResponse.json()
    if (disabled.data.afterCount !== disabled.data.beforeCount - 1) {
      throw new Error('Trigger delete did not decrement favorite count')
    }
    labTriggerAnimeId = undefined

    await clickElementByText('.el-tabs__item', '存储过程')
    await clickElementByText('.el-radio-button', '厂商统计')
    await page.waitForSelector('.el-radio-button.is-active', { timeout: 5000 })
    await setVisibleLabNumberInput(1)
    responsePromise = waitForResponse('GET', '/lab/procedure/studio-statistics/1')
    await clickButton('查询厂商统计')
    const studioResponse = await responsePromise
    await assertResponseOk(studioResponse, 'Studio statistics procedure')
    const studio = await studioResponse.json()
    if (studio.data.studioId !== 1) throw new Error('Studio statistics returned wrong studio')

    await clickElementByText('.el-radio-button', '动漫排行')
    await page.waitForSelector('.el-radio-button.is-active', { timeout: 5000 })
    await setVisibleLabNumberInput(6)
    responsePromise = waitForResponse('GET', '/lab/procedure/anime-ranking')
    await clickButton('查询排行')
    const rankingResponse = await responsePromise
    await assertResponseOk(rankingResponse, 'Anime ranking procedure')
    const ranking = await rankingResponse.json()
    if (ranking.data.length !== 6) {
      throw new Error(`Ranking procedure expected 6 rows, got ${ranking.data.length}`)
    }

    await clickElementByText('.el-tabs__item', '游标')
    responsePromise = waitForResponse('GET', '/lab/cursor/studio-report')
    await clickButton('执行游标')
    const cursorResponse = await responsePromise
    await assertResponseOk(cursorResponse, 'Studio cursor report')
    const cursor = await cursorResponse.json()
    if (cursor.data.length !== 8) {
      throw new Error(`Cursor report expected 8 studios, got ${cursor.data.length}`)
    }

    await clickElementByText('.el-tabs__item', '事务')
    for (const mode of ['COMMIT', 'ROLLBACK', 'EXCEPTION']) {
      responsePromise = waitForResponse('POST', '/lab/transaction/demo')
      await clickElementByText('.transaction-button', mode, false)
      const transactionResponse = await responsePromise
      await assertResponseOk(transactionResponse, `Transaction ${mode}`)
      const payload = await transactionResponse.json()
      if (mode === 'EXCEPTION') {
        if (
          payload.data?.rolledBack !== true ||
          payload.data?.visibleRows !== 0 ||
          !String(payload.data?.error || '').includes('intentional transaction exception')
        ) {
          throw new Error(`Unexpected exception rollback payload: ${JSON.stringify(payload)}`)
        }
      } else {
        if (
          (mode === 'COMMIT' &&
            (payload.data?.committed !== true || payload.data?.visibleRows !== 1)) ||
          (mode === 'ROLLBACK' &&
            (payload.data?.rolledBack !== true || payload.data?.visibleRows !== 0))
        ) {
          throw new Error(`Unexpected ${mode} payload: ${JSON.stringify(payload)}`)
        }
      }
      await page.waitForFunction(
        () =>
          [...document.querySelectorAll('.transaction-button')].every(
            (button) => !button.disabled
          ),
        { timeout: 10000 }
      )
    }

    return [
      `views=${viewRows}`,
      `trigger=${enabled.data.beforeCount}->${enabled.data.afterCount}->${disabled.data.afterCount}`,
      `procedureStudio=${studio.data.studioName}`,
      `ranking=${ranking.data.length}`,
      `cursor=${cursor.data.length}`,
      'transactions=commit/rollback/exception'
    ].join('; ')
  })

  await clickElementByText('.el-tabs__item', '视图查询')
  await wait(300)
  await page.screenshot({ path: path.join(screenshotDir, '06-database-lab.png'), fullPage: true })
  results.push({ name: 'Screenshot 06-database-lab.png', status: 'PASS', route: '/admin/lab' })

  await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 1 })
  await screenshot('/admin/anime', '08-admin-anime-mobile.png', '.admin-page-heading')

  await page.evaluate(() => localStorage.clear())
  await screenshot('/', '07-mobile-home.png', '.anime-card')
} catch (error) {
  issues.push({
    type: 'test',
    url: page.url(),
    message: error instanceof Error ? error.stack || error.message : String(error)
  })
} finally {
  if (token) {
    for (const entry of createdIds) {
      await apiRequest('DELETE', `${entry.endpoint}/${entry.id}`, token).catch(() => undefined)
    }
    if (labTriggerAnimeId) {
      await apiRequest('DELETE', `/user/favorites/${labTriggerAnimeId}`, token).catch(
        () => undefined
      )
    }
  }
  await browser.close()
}

const failed = results.filter((result) => result.status === 'FAIL')
const report = {
  generatedAt: new Date().toISOString(),
  baseUrl,
  results,
  issues,
  warnings,
  passed: failed.length === 0 && issues.length === 0
}
await fs.writeFile(
  path.join(screenshotDir, 'browser-report.json'),
  `${JSON.stringify(report, null, 2)}\n`,
  'utf8'
)

for (const result of results) {
  console.log(`[${result.status}] ${result.name}`)
}
for (const issue of issues) {
  console.error(`[ISSUE] ${issue.type}: ${issue.message}`)
}
for (const warning of warnings) {
  console.warn(`[WARN] ${warning.type}: ${warning.message}`)
}
console.log(
  `Browser test summary: ${results.length - failed.length}/${results.length} checks passed; ` +
    `${issues.length} issue(s); ${warnings.length} warning(s).`
)

if (!report.passed) process.exit(1)

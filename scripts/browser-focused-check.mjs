import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const outputDir = path.join(repoRoot, 'qa-output')
const screenshotDir = path.join(repoRoot, 'docs', 'screenshots')
const baseUrl = process.env.FRONTEND_URL || 'http://127.0.0.1:5173'
const apiUrl = process.env.API_URL || 'http://127.0.0.1:8080/api'
const focus = process.env.BROWSER_FOCUS || 'user'
const chromePath =
  process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const puppeteerModule =
  process.env.PUPPETEER_MODULE ||
  'file:///C:/Users/Chengzi/.codex/tools/mermaid-cli/node_modules/puppeteer/lib/puppeteer/puppeteer.js'

if (!['user', 'admin', 'lab', 'screens'].includes(focus)) {
  throw new Error(
    `BROWSER_FOCUS must be "user", "admin", "lab", or "screens", got "${focus}"`
  )
}

const { default: puppeteer } = await import(
  puppeteerModule.startsWith('file:')
    ? puppeteerModule
    : pathToFileURL(path.resolve(puppeteerModule)).href
)

await fs.mkdir(outputDir, { recursive: true })
await fs.mkdir(screenshotDir, { recursive: true })

const browser = await puppeteer.launch({
  headless: true,
  executablePath: chromePath,
  args: [
    '--no-sandbox',
    '--disable-dev-shm-usage',
    '--disable-gpu',
    '--no-first-run',
    '--no-default-browser-check'
  ]
})
const page = await browser.newPage()
await page.setViewport({ width: 1440, height: 1000, deviceScaleFactor: 1 })

const results = []
const issues = []
const warnings = []
let adminToken
let labTriggerAnimeId
let adminCreated

process.on('unhandledRejection', (reason) => {
  issues.push({
    type: 'unhandledRejection',
    url: page.url(),
    message: reason instanceof Error ? reason.stack || reason.message : String(reason)
  })
})

page.on('pageerror', (error) => {
  issues.push({ type: 'pageerror', url: page.url(), message: error.message })
})
page.on('console', (message) => {
  if (message.type() !== 'error') return
  const locationUrl = message.location()?.url || ''
  const entry = { type: 'console', url: locationUrl || page.url(), message: message.text() }
  if (
    message.text().includes('404') &&
    locationUrl &&
    !locationUrl.startsWith(baseUrl) &&
    !locationUrl.startsWith(apiUrl)
  ) {
    warnings.push(entry)
  } else {
    issues.push(entry)
  }
})
page.on('response', (response) => {
  const responseUrl = response.url()
  if (!responseUrl.includes('/api/') || response.status() < 400) return
  if (
    response.status() === 400 &&
    response.request().method() === 'POST' &&
    responseUrl.includes('/lab/transaction/demo')
  ) {
    return
  }
  issues.push({
    type: 'http',
    url: page.url(),
    message: `${response.status()} ${response.request().method()} ${responseUrl}`
  })
})

const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds))

async function waitForNetwork() {
  await page.waitForNetworkIdle({ idleTime: 300, timeout: 10000 }).catch(() => undefined)
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
      const element = [...document.querySelectorAll(elementSelector)].find((candidate) => {
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

async function setInputByPlaceholder(placeholder, value) {
  await page.waitForSelector(`input[placeholder="${placeholder}"]`, { visible: true })
  await page.evaluate(
    ({ selector, fieldValue }) => {
      const input = document.querySelector(selector)
      if (!(input instanceof HTMLInputElement)) throw new Error(`Input not found: ${selector}`)
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set
      setter?.call(input, String(fieldValue))
      input.dispatchEvent(new Event('input', { bubbles: true }))
      input.dispatchEvent(new Event('change', { bubbles: true }))
      input.dispatchEvent(new FocusEvent('blur', { bubbles: true }))
    },
    { selector: `input[placeholder="${placeholder}"]`, fieldValue: value }
  )
}

async function selectFilterOption(index, label) {
  await page.evaluate((selectIndex) => {
    const select = document.querySelectorAll('.filter-bar .el-select')[selectIndex]
    const trigger = select?.querySelector('.el-select__wrapper') || select
    trigger?.click()
  }, index)
  await page.waitForFunction(
    (optionLabel) =>
      [...document.querySelectorAll('.el-select-dropdown__item')].some(
        (candidate) =>
          candidate.textContent?.trim() === optionLabel &&
          candidate.getBoundingClientRect().height > 0
      ),
    { timeout: 5000 },
    label
  )
  await page.evaluate((optionLabel) => {
    const item = [...document.querySelectorAll('.el-select-dropdown__item')].find(
      (candidate) =>
        candidate.textContent?.trim() === optionLabel &&
        candidate.getBoundingClientRect().height > 0
    )
    item?.click()
  }, label)
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
  if (!updated) throw new Error('Visible lab number input was not found')
}

async function waitForResponse(method, pathFragment) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === method && response.url().includes(pathFragment),
    { timeout: 20000 }
  )
  responsePromise.catch(() => undefined)
  return responsePromise
}

async function assertResponseOk(response, context) {
  if (response.status() >= 400) {
    throw new Error(`${context}: HTTP ${response.status()} ${(await response.text()).slice(0, 500)}`)
  }
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

async function clickRowButton(rowText, buttonText) {
  await page.waitForFunction(
    (value) =>
      [...document.querySelectorAll('tbody tr')].some((row) => row.innerText.includes(value)),
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

async function setDialogField(label, value) {
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
      return { ok: true }
    },
    { fieldLabel: label, fieldValue: value }
  )
  if (!outcome.ok) {
    throw new Error(`Form field not found: ${label}; available=${outcome.labels?.join(', ')}`)
  }
}

async function searchCrudRows(keyword, endpoint) {
  await page.waitForSelector('[data-testid="crud-search"]', { visible: true })
  await page.evaluate(
    ({ selector, value }) => {
      const input = document.querySelector(selector)
      if (!(input instanceof HTMLInputElement)) throw new Error('CRUD search input missing')
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set
      setter?.call(input, value)
      input.dispatchEvent(new Event('input', { bubbles: true }))
      input.dispatchEvent(new Event('change', { bubbles: true }))
    },
    { selector: '[data-testid="crud-search"]', value: keyword }
  )
  const responsePromise = waitForResponse('GET', endpoint)
  await page.click('[data-testid="crud-search-submit"]')
  await assertResponseOk(await responsePromise, `Search ${endpoint}`)
  await wait(250)
}

async function login(username, password, destination) {
  await page.goto(`${baseUrl}/login`, {
    waitUntil: 'domcontentloaded',
    timeout: 30000
  })
  await page.waitForSelector('input[autocomplete="username"]', { visible: true })
  await page.type('input[autocomplete="username"]', username)
  await page.type('input[autocomplete="current-password"]', password)
  await clickButton('登录')
  await page.waitForFunction(
    (expectedPath) =>
      expectedPath
        ? window.location.pathname.startsWith(expectedPath)
        : !window.location.pathname.startsWith('/login'),
    { timeout: 15000 },
    destination
  )
  await wait(400)
}

async function screenshot(route, filename, waitSelector) {
  await page.goto(`${baseUrl}${route}`, {
    waitUntil: 'domcontentloaded',
    timeout: 30000
  })
  if (waitSelector) await page.waitForSelector(waitSelector, { timeout: 20000 })
  await waitForNetwork()
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
  if (overflow) {
    throw new Error(`Horizontal overflow detected on ${route}`)
  }
  return { route, filename }
}

async function runCheck(name, action) {
  console.log(`[RUN] ${name}`)
  try {
    const evidence = await action()
    results.push({ name, status: 'PASS', evidence })
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    results.push({ name, status: 'FAIL', message })
    issues.push({ type: 'test', url: page.url(), message: `${name}: ${message}` })
    throw error
  }
}

async function runUserChecks() {
  await runCheck('Registration validation rejects empty and short usernames', async () => {
    await page.goto(`${baseUrl}/register`, { waitUntil: 'domcontentloaded', timeout: 30000 })
    await page.evaluate(() => localStorage.clear())
    await page.reload({ waitUntil: 'domcontentloaded', timeout: 30000 })
    await page.waitForSelector('input[placeholder="字母、数字或下划线"]', { visible: true })
    await clickButton('创建账号')
    await page.waitForFunction(() => document.body.innerText.includes('请输入用户名'), {
      timeout: 5000
    })
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
    return 'empty required fields and 3-character username rejected client-side'
  })

  await page.evaluate(() => localStorage.clear())
  await login('sakura', 'User123!', '/')

  await runCheck('Home, catalog, pagination, sort, detail, studios, ranking and guard', async () => {
    await page.goto(`${baseUrl}/`, { waitUntil: 'domcontentloaded', timeout: 30000 })
    await page.waitForSelector('.anime-card', { timeout: 15000 })
    const homeCards = await page.$$eval('.anime-card', (elements) => elements.length)
    const rankingItems = await page.$$eval('.ranking-mini', (elements) => elements.length)
    if (homeCards < 1 || rankingItems < 1) {
      throw new Error(`Home content missing: cards=${homeCards}, ranking=${rankingItems}`)
    }

    await page.goto(`${baseUrl}/anime?page=1`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await page.waitForSelector('.anime-card__title', { timeout: 15000 })
    const firstPageTitle = await page.$eval('.anime-card__title', (element) =>
      element.textContent.trim()
    )
    const pageTwoResponse = waitForResponse('GET', '/anime?page=2')
    await clickElementByText('.el-pager li', '2')
    await assertResponseOk(await pageTwoResponse, 'Anime page 2')
    await wait(300)
    const secondPageTitle = await page.$eval('.anime-card__title', (element) =>
      element.textContent.trim()
    )
    if (firstPageTitle === secondPageTitle) {
      throw new Error('Pagination did not change visible records')
    }

    await page.goto(`${baseUrl}/anime?page=1&sort=favorites`, {
      waitUntil: 'domcontentloaded',
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
      if (option.value !== 'favorites') {
        const responsePromise = waitForResponse('GET', `sort=${option.value}`)
        await selectFilterOption(2, option.label)
        await assertResponseOk(await responsePromise, `Sort ${option.value}`)
        await wait(300)
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

    await page.goto(`${baseUrl}/anime/8`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await page.waitForFunction(
      () => document.querySelector('h1')?.textContent?.includes('葬送的芙莉莲'),
      { timeout: 15000 }
    )
    const episodeCount = await page.$$eval('.episode-item', (elements) => elements.length)
    if (episodeCount < 1) throw new Error('Anime detail has no episode controls')

    await page.goto(`${baseUrl}/studios`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await page.waitForSelector('.studio-card', { timeout: 15000 })
    const studioCount = await page.$$eval('.studio-card', (elements) => elements.length)
    if (studioCount !== 8) throw new Error(`Expected 8 studios, got ${studioCount}`)

    await page.goto(`${baseUrl}/ranking`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await page.waitForSelector('.podium-card', { timeout: 15000 })
    const podiumCount = await page.$$eval('.podium-card', (elements) => elements.length)
    if (podiumCount !== 3) throw new Error(`Expected 3 podium cards, got ${podiumCount}`)

    const additionalRoutes = [
      ['/studios/1', '厂商详情'],
      ['/staff', '制作人员'],
      ['/favorites', '我的收藏'],
      ['/watch-history', '观看历史'],
      ['/my-reviews', '我的评论'],
      ['/profile', '个人中心']
    ]
    for (const [route, title] of additionalRoutes) {
      await page.goto(`${baseUrl}${route}`, {
        waitUntil: 'domcontentloaded',
        timeout: 30000
      })
      await page.waitForFunction(
        ({ expectedPath, expectedTitle }) =>
          window.location.pathname === expectedPath &&
          document.title.includes(expectedTitle) &&
          Boolean(document.querySelector('h1')),
        { timeout: 15000 },
        { expectedPath: route, expectedTitle: title }
      )
    }

    await page.goto(`${baseUrl}/admin`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await page.waitForFunction(() => !window.location.pathname.startsWith('/admin'), {
      timeout: 5000
    })
    return [
      `home=${homeCards}/${rankingItems}`,
      `pagination=${firstPageTitle}->${secondPageTitle}`,
      `sorts=${sortEvidence.join('|')}`,
      `episodes=${episodeCount}`,
      `studios=${studioCount}`,
      `podium=${podiumCount}`,
      `additionalRoutes=${additionalRoutes.length}`,
      'adminGuard=redirected'
    ].join('; ')
  })
}

async function runAdminChecks() {
  await login('admin', 'Admin123!', '/admin')
  adminToken = await page.evaluate(() => localStorage.getItem('animeverse_token'))
  if (!adminToken) throw new Error('Admin token was not stored')

  await runCheck('All admin modules render from live API pages', async () => {
    const routes = [
      ['/admin', '管理概览'],
      ['/admin/anime', '动漫管理'],
      ['/admin/episodes', '剧集管理'],
      ['/admin/studios', '厂商管理'],
      ['/admin/genres', '类型管理'],
      ['/admin/staff', '人员管理'],
      ['/admin/platforms', '平台管理'],
      ['/admin/users', '用户管理'],
      ['/admin/roles', '角色管理'],
      ['/admin/reviews', '评论管理'],
      ['/admin/logs', '操作日志'],
      ['/admin/statistics', '数据统计'],
      ['/admin/lab', '数据库实验室']
    ]
    const visited = []
    for (const [route, title] of routes) {
      await page.goto(`${baseUrl}${route}`, {
        waitUntil: 'domcontentloaded',
        timeout: 30000
      })
      await page.waitForFunction(
        ({ expectedPath, expectedTitle }) =>
          window.location.pathname === expectedPath &&
          document.body.innerText.includes(expectedTitle),
        { timeout: 15000 },
        { expectedPath: route, expectedTitle: title }
      )
      visited.push(route)
    }

    await page.goto(`${baseUrl}/admin/statistics`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await page.waitForFunction(
      () => {
        const canvases = [...document.querySelectorAll('canvas')].filter(
          (canvas) => canvas.getBoundingClientRect().height > 0
        )
        return (
          canvases.filter((canvas) =>
            canvas
              .getContext('2d')
              ?.getImageData(0, 0, canvas.width, canvas.height)
              .data.some((value, index) => index % 4 === 3 && value > 0)
          ).length >= 2
        )
      },
      { timeout: 15000 }
    )
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
    const nonblankCharts = chartEvidence.filter((chart) => chart.pixels)
    if (nonblankCharts.length < 2) {
      throw new Error(`Statistics charts are blank: ${JSON.stringify(chartEvidence)}`)
    }
    return `${visited.length} routes; canvases=${chartEvidence.length}; nonblank=${nonblankCharts.length}`
  })

  await runCheck('Genre CRUD works through browser controls', async () => {
    const suffix = Date.now().toString(36).toUpperCase()
    const seedName = `焦点类型${suffix}`
    const updatedName = `${seedName}改`
    await page.goto(`${baseUrl}/admin/genres`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await page.waitForSelector('[data-testid="crud-create"]', { timeout: 15000 })
    await page.click('[data-testid="crud-create"]')
    await setDialogField('类型名称', seedName)
    await setDialogField('类型说明', '第三方独立浏览器复测')
    let responsePromise = waitForResponse('POST', '/admin/genres')
    await page.click('[data-testid="crud-save"]')
    const createdResponse = await responsePromise
    await assertResponseOk(createdResponse, 'Genre create')
    const created = await createdResponse.json()
    const createdId = created.data?.id
    if (createdId === undefined) throw new Error('Genre create did not return an id')
    adminCreated = { endpoint: '/admin/genres', id: createdId }
    await page.waitForSelector('.el-dialog', { hidden: true, timeout: 10000 })

    await searchCrudRows(seedName, '/admin/genres')
    await clickRowButton(seedName, '编辑')
    await setDialogField('类型名称', updatedName)
    await setDialogField('类型说明', '第三方独立浏览器复测已更新')
    responsePromise = waitForResponse('PUT', `/admin/genres/${createdId}`)
    await page.click('[data-testid="crud-save"]')
    await assertResponseOk(await responsePromise, 'Genre update')
    await page.waitForSelector('.el-dialog', { hidden: true, timeout: 10000 })

    await searchCrudRows(updatedName, '/admin/genres')
    await clickRowButton(updatedName, '删除')
    await page.waitForSelector('.el-message-box', { visible: true, timeout: 10000 })
    responsePromise = waitForResponse('DELETE', `/admin/genres/${createdId}`)
    await clickButton('确认删除', '.el-message-box')
    await assertResponseOk(await responsePromise, 'Genre delete')
    adminCreated = undefined
    return `id=${createdId}; create/search/update/delete`
  })

  await runCheck('User role dialog restores persisted USER role', async () => {
    const roles = await apiRequest('GET', '/admin/roles', adminToken)
    const userRole = roles.data.find((role) => role.code === 'USER')
    if (!userRole) throw new Error('USER role not found')
    await page.goto(`${baseUrl}/admin/users`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await setInputByPlaceholder('搜索用户名、昵称或邮箱', 'sakura')
    const searchResponse = waitForResponse('GET', '/admin/users')
    await clickButton('搜索', '.admin-toolbar')
    await assertResponseOk(await searchResponse, 'Search sakura')
    await clickRowButton('sakura', '分配角色')
    await page.waitForSelector('.el-dialog', { visible: true, timeout: 10000 })
    const selectedText = await page.$eval('.el-dialog .el-select', (element) =>
      element.textContent.trim()
    )
    const expectedRoleName = userRole.name || userRole.roleName || userRole.code
    if (!selectedText.includes(expectedRoleName)) {
      throw new Error(`Role was not restored: selected="${selectedText}"`)
    }
    const saveResponse = waitForResponse('PUT', '/admin/users/')
    await clickButton('保存', '.el-dialog')
    await assertResponseOk(await saveResponse, 'Save unchanged role')
    return `selected=${expectedRoleName}; unchanged save`
  })

  await runCheck('Review visibility and delete controls work through browser', async () => {
    const userToken = await apiLogin('sakura', 'User123!')
    const mine = await apiRequest(
      'GET',
      '/user/reviews?page=1&size=100',
      userToken
    )
    const reviewedIds = new Set(
      (mine.data.records || []).map((review) => Number(review.animeId))
    )
    const animeId = Array.from({ length: 30 }, (_, index) => index + 1).find(
      (candidate) => !reviewedIds.has(candidate)
    )
    if (!animeId) throw new Error('No unreviewed anime available for review controls')
    const content = `第三方评论复核${Date.now().toString(36)}`
    const created = await apiRequest(
      'POST',
      `/anime/${animeId}/reviews`,
      userToken,
      { rating: 8, content }
    )
    const reviewId = created.data?.id
    if (reviewId === undefined) throw new Error('Temporary review create returned no id')
    adminCreated = { endpoint: '/admin/reviews', id: reviewId }

    await page.goto(`${baseUrl}/admin/reviews`, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    })
    await setInputByPlaceholder('搜索评论内容、用户或动漫', content)
    let responsePromise = waitForResponse('GET', '/admin/reviews')
    await clickButton('搜索', '.admin-toolbar')
    await assertResponseOk(await responsePromise, 'Search temporary review')
    await page.waitForFunction(
      (needle) => document.body.innerText.includes(needle),
      { timeout: 10000 },
      content
    )

    responsePromise = waitForResponse('PUT', `/admin/reviews/${reviewId}/visibility`)
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

    responsePromise = waitForResponse('DELETE', `/admin/reviews/${reviewId}`)
    await clickRowButton(content, '删除')
    await page.waitForSelector('.el-message-box', { visible: true, timeout: 10000 })
    await clickButton('确认删除', '.el-message-box')
    await assertResponseOk(await responsePromise, 'Delete review')
    adminCreated = undefined
    return `id=${reviewId}; visible/hidden/visible/delete`
  })
}

async function runLabChecks() {
  await login('admin', 'Admin123!', '/admin')
  adminToken = await page.evaluate(() => localStorage.getItem('animeverse_token'))
  if (!adminToken) throw new Error('Admin token was not stored')
  await page.goto(`${baseUrl}/admin/lab`, {
    waitUntil: 'domcontentloaded',
    timeout: 30000
  })

  await runCheck('Database lab five tabs execute real database objects', async () => {
    await page.waitForSelector('.lab-tabs', { timeout: 15000 })
    let responsePromise = waitForResponse('GET', '/lab/views/v_anime_detail')
    await clickButton('执行查询')
    await assertResponseOk(await responsePromise, 'View query')
    await page.waitForSelector('.lab-panel .el-table__row', { timeout: 15000 })
    const viewRows = await page.$$eval('.lab-panel .el-table__row', (elements) => elements.length)

    const favorites = await apiRequest('GET', '/user/favorites', adminToken)
    const favoriteIds = new Set((favorites.data || []).map((item) => Number(item.animeId)))
    const animeId = Array.from({ length: 30 }, (_, index) => index + 1).find(
      (candidate) => !favoriteIds.has(candidate)
    )
    if (!animeId) throw new Error('No unfavorited anime available')

    await clickElementByText('.el-tabs__item', '触发器')
    await page.waitForFunction(
      () =>
        [...document.querySelectorAll('.el-tab-pane')].some(
          (pane) =>
            pane.getBoundingClientRect().height > 0 &&
            pane.textContent?.includes('收藏计数触发器')
        ),
      { timeout: 5000 }
    )
    await setVisibleLabNumberInput(animeId)
    responsePromise = waitForResponse('POST', '/lab/trigger/favorite')
    await clickButton('执行触发器')
    const enabledResponse = await responsePromise
    await assertResponseOk(enabledResponse, 'Trigger enable')
    const enabled = await enabledResponse.json()
    if (enabled.data.afterCount !== enabled.data.beforeCount + 1) {
      throw new Error('Favorite trigger did not increment')
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
    await assertResponseOk(disabledResponse, 'Trigger disable')
    const disabled = await disabledResponse.json()
    if (disabled.data.afterCount !== disabled.data.beforeCount - 1) {
      throw new Error('Favorite trigger did not decrement')
    }
    labTriggerAnimeId = undefined

    await clickElementByText('.el-tabs__item', '存储过程')
    await clickElementByText('.el-radio-button', '厂商统计')
    await page.waitForSelector('.el-radio-button.is-active', { timeout: 5000 })
    await setVisibleLabNumberInput(1)
    responsePromise = waitForResponse('GET', '/lab/procedure/studio-statistics/1')
    await clickButton('查询厂商统计')
    const studioResponse = await responsePromise
    await assertResponseOk(studioResponse, 'Studio statistics')
    const studio = await studioResponse.json()
    if (studio.data.studioId !== 1) throw new Error('Wrong studio statistics result')

    await clickElementByText('.el-radio-button', '动漫排行')
    await page.waitForSelector('.el-radio-button.is-active', { timeout: 5000 })
    await setVisibleLabNumberInput(6)
    responsePromise = waitForResponse('GET', '/lab/procedure/anime-ranking')
    await clickButton('查询排行')
    const rankingResponse = await responsePromise
    await assertResponseOk(rankingResponse, 'Anime ranking')
    const ranking = await rankingResponse.json()
    if (ranking.data.length !== 6) {
      throw new Error(`Ranking expected 6 rows, got ${ranking.data.length}`)
    }

    await clickElementByText('.el-tabs__item', '游标')
    responsePromise = waitForResponse('GET', '/lab/cursor/studio-report')
    await clickButton('执行游标')
    const cursorResponse = await responsePromise
    await assertResponseOk(cursorResponse, 'Cursor report')
    const cursor = await cursorResponse.json()
    if (cursor.data.length !== 8) {
      throw new Error(`Cursor expected 8 rows, got ${cursor.data.length}`)
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
      } else if (mode === 'COMMIT') {
        if (payload.data?.committed !== true || payload.data?.visibleRows !== 1) {
          throw new Error(`Unexpected commit payload: ${JSON.stringify(payload)}`)
        }
      } else if (mode === 'ROLLBACK') {
        if (payload.data?.rolledBack !== true || payload.data?.visibleRows !== 0) {
          throw new Error(`Unexpected rollback payload: ${JSON.stringify(payload)}`)
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

    await clickElementByText('.el-tabs__item', '视图查询')
    await wait(250)
    await page.screenshot({
      path: path.join(outputDir, 'database-lab-focused.png'),
      fullPage: true
    })
    return [
      `views=${viewRows}`,
      `trigger=${enabled.data.beforeCount}->${enabled.data.afterCount}->${disabled.data.afterCount}`,
      `studio=${studio.data.studioName}`,
      `ranking=${ranking.data.length}`,
      `cursor=${cursor.data.length}`,
      'transactions=commit/rollback/exception'
    ].join('; ')
  })
}

async function runScreenChecks() {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.evaluate(() => localStorage.clear())
  await login('sakura', 'User123!', '/')

  await page.setViewport({ width: 1440, height: 1000, deviceScaleFactor: 1 })
  await runCheck('Desktop user screenshots come from live pages', async () => {
    const shots = []
    shots.push(await screenshot('/', '01-home.png', '.anime-card'))
    shots.push(await screenshot('/anime', '02-anime-list.png', '.anime-card'))
    shots.push(await screenshot('/anime/8', '03-anime-detail.png', 'h1'))
    shots.push(await screenshot('/ranking', '04-ranking.png', '.podium-card'))
    return shots.map((item) => item.filename).join(', ')
  })

  await page.evaluate(() => localStorage.clear())
  await login('admin', 'Admin123!', '/admin')

  await runCheck('Desktop admin screenshots come from live pages', async () => {
    const shots = []
    shots.push(await screenshot('/admin', '05-admin-dashboard.png', '.admin-page-heading'))
    shots.push(await screenshot('/admin/lab', '06-database-lab.png', '.lab-tabs'))
    return shots.map((item) => item.filename).join(', ')
  })

  await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 1 })
  await runCheck('Mobile admin screenshot has no horizontal overflow', async () => {
    const shot = await screenshot(
      '/admin/anime',
      '08-admin-anime-mobile.png',
      '.admin-page-heading'
    )
    return shot.filename
  })

  await page.evaluate(() => localStorage.clear())
  await login('sakura', 'User123!', '/')
  await runCheck('Mobile user screenshot has no horizontal overflow', async () => {
    const shot = await screenshot('/', '07-mobile-home.png', '.anime-card')
    return shot.filename
  })
}

try {
  if (focus === 'user') {
    await runUserChecks()
  } else if (focus === 'admin') {
    await runAdminChecks()
  } else if (focus === 'lab') {
    await runLabChecks()
  } else {
    await runScreenChecks()
  }
} catch (error) {
  issues.push({
    type: 'test',
    url: page.url(),
    message: error instanceof Error ? error.stack || error.message : String(error)
  })
} finally {
  if (adminToken && labTriggerAnimeId) {
    await apiRequest('DELETE', `/user/favorites/${labTriggerAnimeId}`, adminToken).catch(
      () => undefined
    )
  }
  if (adminToken && adminCreated) {
    await apiRequest('DELETE', `${adminCreated.endpoint}/${adminCreated.id}`, adminToken).catch(
      () => undefined
    )
  }
  try {
    await browser.close()
  } catch (error) {
    warnings.push({
      type: 'browser-close',
      url: page.url(),
      message: error instanceof Error ? error.message : String(error)
    })
  }
}

const failed = results.filter((result) => result.status === 'FAIL')
const report = {
  generatedAt: new Date().toISOString(),
  focus,
  baseUrl,
  results,
  issues,
  warnings,
  passed: failed.length === 0 && issues.length === 0
}
const reportPath = path.join(outputDir, `browser-${focus}-report.json`)
await fs.writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
if (focus === 'screens') {
  const focusedSuites = {}
  for (const suite of ['user', 'admin', 'lab']) {
    const suitePath = path.join(outputDir, `browser-${suite}-report.json`)
    try {
      focusedSuites[suite] = JSON.parse(await fs.readFile(suitePath, 'utf8'))
    } catch {
      focusedSuites[suite] = null
    }
  }
  await fs.writeFile(
    path.join(screenshotDir, 'browser-report.json'),
    `${JSON.stringify({ ...report, focusedSuites }, null, 2)}\n`,
    'utf8'
  )
}

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
  `Focused browser summary (${focus}): ${results.length - failed.length}/${results.length} checks passed; ` +
    `${issues.length} issue(s); ${warnings.length} warning(s).`
)

if (!report.passed) process.exit(1)

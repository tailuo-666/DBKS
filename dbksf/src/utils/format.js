export const ROLE_USER = '\u7528\u6237\u7aef'
export const ROLE_ADMIN = '\u7ba1\u7406\u7aef'
const LEGACY_ROLE_ADMIN = '\u7ba1\u7406\u5458'

export const PRODUCT_STATUS_ON_SHELF = '\u5df2\u4e0a\u67b6'
export const PRODUCT_STATUS_OFF_SHELF = '\u5df2\u4e0b\u67b6'
export const PRODUCT_STATUS_REVIEWING = '\u5ba1\u6838\u4e2d'

export const REPORT_STATUS_PENDING = '\u5f85\u5904\u7406'
export const REPORT_STATUS_PROCESSED = '\u5df2\u5904\u7406'

export const CATEGORY_OPTIONS = [
  '\u4e8c\u624b\u4e66',
  '\u95f2\u7f6e\u7269\u54c1',
  '\u7535\u5b50\u4ea7\u54c1',
  '\u65e5\u7528\u54c1',
]

export const EDITABLE_PRODUCT_STATUS_OPTIONS = [
  PRODUCT_STATUS_ON_SHELF,
  PRODUCT_STATUS_OFF_SHELF,
]

export function normalizeRole(role) {
  if (role === LEGACY_ROLE_ADMIN) {
    return ROLE_ADMIN
  }

  if (role === ROLE_USER || role === ROLE_ADMIN) {
    return role
  }

  return role || ''
}

export function isAdminRole(role) {
  return normalizeRole(role) === ROLE_ADMIN
}

export function isUserRole(role) {
  return normalizeRole(role) === ROLE_USER
}

export function getDefaultRouteByRole(role) {
  return isAdminRole(role) ? '/admin/review' : '/user/shop'
}

export function resolveSafeRedirect(role, redirect) {
  const normalizedRole = normalizeRole(role)
  if (typeof redirect !== 'string' || !redirect) {
    return getDefaultRouteByRole(normalizedRole)
  }

  if (normalizedRole === ROLE_USER && redirect.startsWith('/user/')) {
    return redirect
  }

  if (normalizedRole === ROLE_ADMIN && redirect.startsWith('/admin/')) {
    return redirect
  }

  return getDefaultRouteByRole(normalizedRole)
}

export function normalizeTagList(tags) {
  if (Array.isArray(tags)) {
    return [...new Set(tags.map((tag) => String(tag).trim()).filter(Boolean))]
  }

  if (typeof tags === 'string') {
    return [...new Set(tags.split(/\s+/).map((tag) => tag.trim()).filter(Boolean))]
  }

  return []
}

export function serializeTags(tags) {
  return normalizeTagList(tags).join(' ')
}

export function normalizeImageUrls(imageUrls) {
  if (!Array.isArray(imageUrls)) {
    return []
  }

  return imageUrls
    .map((url) => String(url || '').trim())
    .filter(Boolean)
}

export function formatCurrency(value) {
  const amount = Number(value || 0)
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
  }).format(Number.isFinite(amount) ? amount : 0)
}

export function formatDateTime(value) {
  if (!value) {
    return '\u2014'
  }

  const parsedDate = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(parsedDate.getTime())) {
    return String(value)
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsedDate)
}

export function formatProductSummary(text, maxLength = 72) {
  const normalized = String(text || '').trim()
  if (!normalized) {
    return '\u6682\u65e0\u63cf\u8ff0'
  }

  return normalized.length > maxLength
    ? `${normalized.slice(0, maxLength).trim()}...`
    : normalized
}

export function mapStatusToTagType(status) {
  switch (status) {
    case PRODUCT_STATUS_ON_SHELF:
      return 'success'
    case PRODUCT_STATUS_OFF_SHELF:
      return 'info'
    case PRODUCT_STATUS_REVIEWING:
      return 'warning'
    case REPORT_STATUS_PENDING:
      return 'danger'
    case REPORT_STATUS_PROCESSED:
      return ''
    default:
      return 'info'
  }
}

export function createEmptyProductDraft(overrides = {}) {
  return {
    currentStatus: '',
    category: '',
    name: '',
    imageUrls: [''],
    description: '',
    price: null,
    wechat: '',
    address: '',
    tags: [],
    status: '',
    ...overrides,
  }
}

export function buildDraftFromPublishedProduct(item, detail) {
  const imageUrls = normalizeImageUrls(detail?.imageUrls)

  return createEmptyProductDraft({
    currentStatus: item?.status || PRODUCT_STATUS_ON_SHELF,
    category: detail?.category || '',
    name: item?.name || detail?.name || '',
    imageUrls: imageUrls.length ? imageUrls : [item?.imageUrl || ''],
    description: detail?.description || item?.description || '',
    price: detail?.price != null ? Number(detail.price) : null,
    wechat: detail?.wechat || '',
    address: detail?.address || '',
    tags: normalizeTagList(item?.tags),
    status: EDITABLE_PRODUCT_STATUS_OPTIONS.includes(item?.status) ? item.status : '',
  })
}

export function buildDraftFromMineItem(item) {
  return createEmptyProductDraft({
    currentStatus: item?.status || '',
    name: item?.name || '',
    imageUrls: item?.imageUrl ? [item.imageUrl] : [''],
    description: item?.description || '',
    tags: normalizeTagList(item?.tags),
    status: EDITABLE_PRODUCT_STATUS_OPTIONS.includes(item?.status) ? item.status : '',
  })
}

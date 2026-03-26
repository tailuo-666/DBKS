export const ROLE_USER = '用户端'
export const ROLE_ADMIN = '管理员'

export const PRODUCT_STATUS_ON_SHELF = '已上架'
export const PRODUCT_STATUS_OFF_SHELF = '已下架'
export const PRODUCT_STATUS_REVIEWING = '审核中'

export const REPORT_STATUS_PENDING = '待处理'
export const REPORT_STATUS_PROCESSED = '已处理'

export const CATEGORY_OPTIONS = ['二手书', '闲置物品', '电子产品', '日用品']
export const EDITABLE_PRODUCT_STATUS_OPTIONS = [
  PRODUCT_STATUS_ON_SHELF,
  PRODUCT_STATUS_OFF_SHELF,
]

export function getDefaultRouteByRole(role) {
  return role === ROLE_ADMIN ? '/admin/review' : '/user/shop'
}

export function resolveSafeRedirect(role, redirect) {
  if (typeof redirect !== 'string' || !redirect) {
    return getDefaultRouteByRole(role)
  }

  if (role === ROLE_USER && redirect.startsWith('/user/')) {
    return redirect
  }

  if (role === ROLE_ADMIN && redirect.startsWith('/admin/')) {
    return redirect
  }

  return getDefaultRouteByRole(role)
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
    return '—'
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
    return '暂无描述'
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

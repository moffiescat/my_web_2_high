import request from './request'

export function getCartList() {
  return request({ url: '/cart/list', method: 'get' })
}

export function addToCart(goodsId) {
  return request({ url: '/cart/add', method: 'post', data: { goodsId } })
}

export function updateCartQuantity(cartId, quantity) {
  return request({ url: `/cart/${cartId}`, method: 'put', data: { quantity } })
}

export function removeFromCart(cartId) {
  return request({ url: `/cart/${cartId}`, method: 'delete' })
}

export function clearCart() {
  return request({ url: '/cart/clear', method: 'delete' })
}

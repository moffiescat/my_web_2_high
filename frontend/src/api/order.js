import request from './request'

export function getOrderList() {
  return request({ url: '/order/list', method: 'get' })
}

export function getOrderDetail(orderId) {
  return request({ url: `/order/detail/${orderId}`, method: 'get' })
}

export function cancelOrder(orderId) {
  return request({ url: `/order/cancel/${orderId}`, method: 'post' })
}

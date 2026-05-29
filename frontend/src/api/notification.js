import request from './request'

export function getNotifications() {
  return request({ url: '/notification/list', method: 'get' })
}

export function markAllRead() {
  return request({ url: '/notification/read-all', method: 'put' })
}

import request from './request'

export function login(phone, password) {
  return request({ url: '/user/login', method: 'post', data: { phone, password } })
}

export function register(nickname, phone, password) {
  return request({ url: '/user/register', method: 'post', data: { nickname, phone, password } })
}

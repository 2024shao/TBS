import request from './request'

export function joinMatch(userId, username, assistantId) {
  return request({
    url: '/match/join',
    method: 'post',
    data: { userId, username, assistantId }
  })
}

export function cancelMatch(userId) {
  return request({
    url: '/match/cancel',
    method: 'post',
    data: { userId }
  })
}

export function checkMatchStatus(userId) {
  return request({
    url: '/match/status',
    method: 'get',
    params: { userId }
  })
}
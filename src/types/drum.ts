export type Sound = 'bass' | 'edge' | 'click'

export interface Line {
  count: number
  sound: Sound
  dots: boolean[]
  muted: boolean
}

export interface Strike {
  sound: Sound
  line: 'top' | 'bottom'
  id: number
}

export interface Strikes {
  top: Strike | null
  bottom: Strike | null
}

export interface Swing {
  id: number
  rot: number
  x: string
  y: string
}

export interface Ripple {
  id: number
  x: number
  y: number
  color: string
}

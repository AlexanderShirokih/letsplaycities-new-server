package game

import (
	"encoding/json"
	"errors"
	"fmt"
)

// Message types. Client and server share the names "word" and "chat".
const (
	TypeHello       = "hello"
	TypeFind        = "find"
	TypeInvite      = "invite"
	TypeInviteReply = "inviteReply"
	TypeCancel      = "cancel"
	TypeWord        = "word"
	TypeChat        = "chat"
	TypeLeave       = "leave"

	TypeWelcome      = "welcome"
	TypeQueued       = "queued"
	TypeInviteStatus = "inviteStatus"
	TypeMatch        = "match"
	TypeGameOver     = "gameOver"
	TypeError        = "error"
)

// Word statuses.
const (
	WordAccepted    = "accepted"
	WordAlreadyUsed = "alreadyUsed"
	WordUnknown     = "unknown"
	WordWrongLetter = "wrongLetter"
	WordNotYourTurn = "notYourTurn"
)

// Game-over reasons.
const (
	ReasonTimeout      = "timeout"
	ReasonOpponentLeft = "opponentLeft"
	ReasonSurrender    = "surrender"
)

// Invite statuses.
const (
	InviteSent       = "sent"
	InviteDeclined   = "declined"
	InviteBusy       = "busy"
	InviteOffline    = "offline"
	InviteNotFriends = "notFriends"
	InviteBlocked    = "blocked"
	InviteExpired    = "expired"
)

// Error codes.
const (
	ErrCodeBadMessage          = "bad_message"
	ErrCodeHelloRequired       = "hello_required"
	ErrCodeInvalidState        = "invalid_state"
	ErrCodeUnsupportedProtocol = "unsupported_protocol"
	ErrCodeReplaced            = "replaced"
)

// ErrBadMessage is returned by DecodeClient for malformed JSON and for a
// missing or unknown message type.
var ErrBadMessage = errors.New("bad message")

var clientTypes = map[string]struct{}{
	TypeHello:       {},
	TypeFind:        {},
	TypeInvite:      {},
	TypeInviteReply: {},
	TypeCancel:      {},
	TypeWord:        {},
	TypeChat:        {},
	TypeLeave:       {},
}

// ClientMsg is any message sent by a client. Which fields are meaningful
// depends on Type; the hub validates them.
type ClientMsg struct {
	Type     string `json:"type"`
	Protocol int    `json:"protocol,omitempty"`
	Client   string `json:"client,omitempty"`
	Name     string `json:"name,omitempty"`
	UserID   int64  `json:"userId,omitempty"`
	Accept   bool   `json:"accept,omitempty"`
	Text     string `json:"text,omitempty"`
}

// DecodeClient parses a client frame. It only checks that the frame is a
// JSON object with a known type.
func DecodeClient(b []byte) (ClientMsg, error) {
	var m ClientMsg

	if err := json.Unmarshal(b, &m); err != nil {
		return ClientMsg{}, fmt.Errorf("%w: %w", ErrBadMessage, err)
	}

	if _, ok := clientTypes[m.Type]; !ok {
		return ClientMsg{}, fmt.Errorf("%w: unknown type %q", ErrBadMessage, m.Type)
	}

	return m, nil
}

// UserView is the public profile of a user. AvatarHash is null when the
// user has no avatar.
type UserView struct {
	ID         int64   `json:"id"`
	Name       string  `json:"name"`
	AvatarHash *string `json:"avatarHash"`
	Provider   string  `json:"provider,omitempty"`
	LastSeenAt int64   `json:"lastSeenAt,omitempty"`
}

// Opponent describes the other player in a match.
type Opponent struct {
	ID         int64   `json:"id"`
	Name       string  `json:"name"`
	AvatarHash *string `json:"avatarHash"`
	Friendship string  `json:"friendship"`
}

// Server messages. The Type field comes first so that "type" is the first
// key of every encoded frame.
type (
	welcomeMsg struct {
		Type string   `json:"type"`
		User UserView `json:"user"`
	}

	typeOnlyMsg struct {
		Type string `json:"type"`
	}

	inviteStatusMsg struct {
		Type   string `json:"type"`
		UserID int64  `json:"userId"`
		Status string `json:"status"`
	}

	matchMsg struct {
		Type        string   `json:"type"`
		GameID      int64    `json:"gameId"`
		Opponent    Opponent `json:"opponent"`
		YouStart    bool     `json:"youStart"`
		MoveTimeSec int      `json:"moveTimeSec"`
	}

	textMsg struct {
		Type   string `json:"type"`
		By     int64  `json:"by"`
		Text   string `json:"text"`
		Status string `json:"status,omitempty"`
	}

	gameOverMsg struct {
		Type        string `json:"type"`
		Reason      string `json:"reason"`
		Loser       int64  `json:"loser"`
		Words       int    `json:"words"`
		DurationSec int    `json:"durationSec"`
	}

	errorMsg struct {
		Type string `json:"type"`
		Code string `json:"code"`
	}
)

// encode marshals a server message. The message types above contain only
// strings and numbers, so marshaling cannot fail.
func encode(v any) []byte {
	b, err := json.Marshal(v)
	if err != nil {
		panic(fmt.Sprintf("game: encode %T: %v", v, err))
	}

	return b
}

// Welcome is sent after a successful hello.
func Welcome(u UserView) []byte {
	return encode(welcomeMsg{Type: TypeWelcome, User: u})
}

// Queued confirms that the player entered the matchmaking queue.
func Queued() []byte {
	return encode(typeOnlyMsg{Type: TypeQueued})
}

// InviteStatus reports the state of an invitation concerning userID.
func InviteStatus(userID int64, status string) []byte {
	return encode(inviteStatusMsg{Type: TypeInviteStatus, UserID: userID, Status: status})
}

// Match announces a created game.
func Match(gameID int64, opp Opponent, youStart bool, moveTimeSec int) []byte {
	return encode(matchMsg{
		Type: TypeMatch, GameID: gameID, Opponent: opp,
		YouStart: youStart, MoveTimeSec: moveTimeSec,
	})
}

// Word reports a move made by player by and the verdict on it.
func Word(by int64, text, status string) []byte {
	return encode(textMsg{Type: TypeWord, By: by, Text: text, Status: status})
}

// Chat delivers a chat message written by player by.
func Chat(by int64, text string) []byte {
	return encode(textMsg{Type: TypeChat, By: by, Text: text})
}

// GameOver ends the game.
func GameOver(reason string, loser int64, words, durationSec int) []byte {
	return encode(gameOverMsg{
		Type: TypeGameOver, Reason: reason, Loser: loser,
		Words: words, DurationSec: durationSec,
	})
}

// Error reports a protocol error with a machine-readable code.
func Error(code string) []byte {
	return encode(errorMsg{Type: TypeError, Code: code})
}

package game

import (
	"bytes"
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"reflect"
	"strings"
	"testing"
)

const fixturesDir = "../../testdata/protocol"

func readFixture(t *testing.T, name string) []byte {
	t.Helper()

	b, err := os.ReadFile(filepath.Join(fixturesDir, name))
	if err != nil {
		t.Fatalf("read fixture %s: %v", name, err)
	}

	return b
}

func toMap(t *testing.T, b []byte) map[string]any {
	t.Helper()

	var m map[string]any
	if err := json.Unmarshal(b, &m); err != nil {
		t.Fatalf("unmarshal %s: %v", b, err)
	}

	return m
}

func fixtureNames(t *testing.T, prefix string) []string {
	t.Helper()

	files, err := filepath.Glob(filepath.Join(fixturesDir, prefix+"-*.json"))
	if err != nil {
		t.Fatal(err)
	}

	names := make([]string, 0, len(files))
	for _, f := range files {
		base := filepath.Base(f)
		names = append(names, strings.TrimSuffix(strings.TrimPrefix(base, prefix+"-"), ".json"))
	}

	return names
}

func TestServerFixtures(t *testing.T) {
	avatar := "3f7a9c1be2d84a55"
	built := map[string][]byte{
		"welcome": Welcome(UserView{
			ID: 14397, Name: "Мария", AvatarHash: &avatar,
			Provider: "google", LastSeenAt: 1762560000000,
		}),
		"queued":       Queued(),
		"inviteStatus": InviteStatus(12407, InviteSent),
		"match": Match(58231, Opponent{
			ID: 12408, Name: "Дмитрий", Friendship: "accepted",
		}, true, 92),
		"word":     Word(14397, "Архангельск", WordAccepted),
		"chat":     Chat(12408, "Хороший ход!"),
		"gameOver": GameOver(ReasonTimeout, 12408, 17, 412),
		"error":    Error(ErrCodeBadMessage),
	}

	names := fixtureNames(t, "server")
	if len(names) != len(built) {
		t.Fatalf("fixtures %v, constructors %d", names, len(built))
	}

	for _, name := range names {
		got, ok := built[name]
		if !ok {
			t.Errorf("no constructor for fixture server-%s", name)

			continue
		}

		want := toMap(t, readFixture(t, "server-"+name+".json"))
		if !reflect.DeepEqual(toMap(t, got), want) {
			t.Errorf("server-%s: got %s, want %v", name, got, want)
		}

		if !bytes.HasPrefix(got, []byte(`{"type":"`+name+`"`)) {
			t.Errorf("server-%s: type is not the first key: %s", name, got)
		}
	}
}

func TestClientFixtures(t *testing.T) {
	names := fixtureNames(t, "client")
	if len(names) != len(clientTypes) {
		t.Fatalf("fixtures %v, client types %d", names, len(clientTypes))
	}

	for _, name := range names {
		msg, err := DecodeClient(readFixture(t, "client-"+name+".json"))
		if err != nil {
			t.Errorf("client-%s: %v", name, err)

			continue
		}

		if msg.Type != name {
			t.Errorf("client-%s: type %q", name, msg.Type)
		}
	}
}

func TestDecodeClientFields(t *testing.T) {
	msg, err := DecodeClient(readFixture(t, "client-hello.json"))
	if err != nil {
		t.Fatal(err)
	}

	want := ClientMsg{Type: TypeHello, Protocol: 1, Client: "lps-flutter/2.0.0", Name: "Александр"}
	if msg != want {
		t.Errorf("got %+v, want %+v", msg, want)
	}
}

func TestDecodeBadMessage(t *testing.T) {
	for _, in := range []string{"{", `{"type":"fly"}`, `{}`, `{"type":""}`, `[]`, ``} {
		_, err := DecodeClient([]byte(in))
		if !errors.Is(err, ErrBadMessage) {
			t.Errorf("DecodeClient(%q) error = %v, want ErrBadMessage", in, err)
		}
	}
}

func TestAvatarHashAlwaysPresent(t *testing.T) {
	m := toMap(t, Welcome(UserView{ID: 1, Name: "a"}))

	user, _ := m["user"].(map[string]any)
	if v, ok := user["avatarHash"]; !ok || v != nil {
		t.Errorf("user.avatarHash = %v (present %v), want null", v, ok)
	}

	if _, ok := user["provider"]; ok {
		t.Error("provider must be omitted when empty")
	}
}

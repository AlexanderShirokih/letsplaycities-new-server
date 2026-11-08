package config

import (
	"reflect"
	"strings"
	"testing"
	"time"
)

func env(m map[string]string) func(string) string {
	return func(k string) string { return m[k] }
}

func base() map[string]string {
	return map[string]string{
		"LPS_DATA_DIR":      "/data",
		"LPS_NATIVE_PEPPER": "x",
		"LPS_TLS_CERT":      "c.pem",
		"LPS_TLS_KEY":       "k.pem",
	}
}

func TestLoadDefaults(t *testing.T) {
	c, err := Load(env(base()))
	if err != nil {
		t.Fatal(err)
	}

	if c.MoveTime != 92*time.Second || c.PingInterval != 5*time.Second ||
		c.PingTimeout != 15*time.Second || c.InviteTTL != 60*time.Second ||
		c.BotsWait != 15*time.Second {
		t.Fatalf("bad durations: %+v", c)
	}

	if c.Addr != ":443" {
		t.Fatalf("addr = %q", c.Addr)
	}

	if c.TLS() != TLSFiles {
		t.Fatal("want TLSFiles")
	}
}

func TestLoadRequiresPepper(t *testing.T) {
	m := base()
	delete(m, "LPS_NATIVE_PEPPER")

	_, err := Load(env(m))
	if err == nil || !strings.Contains(err.Error(), "LPS_NATIVE_PEPPER") {
		t.Fatalf("err = %v", err)
	}
}

func TestLoadRequiresTLS(t *testing.T) {
	m := base()
	delete(m, "LPS_TLS_CERT")
	delete(m, "LPS_TLS_KEY")

	if _, err := Load(env(m)); err == nil {
		t.Fatal("want error")
	}
}

func TestLoadAutocert(t *testing.T) {
	m := base()
	delete(m, "LPS_TLS_CERT")
	delete(m, "LPS_TLS_KEY")
	m["LPS_DOMAIN"] = "example.org"

	c, err := Load(env(m))
	if err != nil {
		t.Fatal(err)
	}

	if c.TLS() != TLSAutocert {
		t.Fatal("want TLSAutocert")
	}
}

func TestLoadGoogleIDs(t *testing.T) {
	m := base()
	m["LPS_GOOGLE_CLIENT_IDS"] = "a, b"

	c, err := Load(env(m))
	if err != nil {
		t.Fatal(err)
	}

	if !reflect.DeepEqual(c.GoogleClientIDs, []string{"a", "b"}) {
		t.Fatalf("ids = %#v", c.GoogleClientIDs)
	}
}

func TestLoadBadDuration(t *testing.T) {
	m := base()
	m["LPS_MOVE_TIME_SEC"] = "abc"

	_, err := Load(env(m))
	if err == nil || !strings.Contains(err.Error(), "LPS_MOVE_TIME_SEC") {
		t.Fatalf("err = %v", err)
	}
}

func TestLoadDurationMinimums(t *testing.T) {
	for _, name := range []string{"LPS_MOVE_TIME_SEC", "LPS_INVITE_TTL_SEC"} {
		m := base()
		m[name] = "0"

		_, err := Load(env(m))
		if err == nil || !strings.Contains(err.Error(), name) {
			t.Fatalf("%s=0: err = %v", name, err)
		}

		m[name] = "1"

		if _, err := Load(env(m)); err != nil {
			t.Fatalf("%s=1: %v", name, err)
		}
	}

	m := base()
	m["LPS_BOTS_WAIT_SEC"] = "0"

	if _, err := Load(env(m)); err != nil {
		t.Fatalf("LPS_BOTS_WAIT_SEC=0: %v", err)
	}
}

func TestLoadDomainWinsOverFiles(t *testing.T) {
	m := base()
	m["LPS_DOMAIN"] = "example.org"

	c, err := Load(env(m))
	if err != nil {
		t.Fatal(err)
	}

	if c.TLS() != TLSAutocert {
		t.Fatal("want TLSAutocert when domain and files are both set")
	}
}

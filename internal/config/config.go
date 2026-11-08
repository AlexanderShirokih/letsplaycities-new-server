// Package config reads server configuration from LPS_* environment variables.
package config

import (
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"
)

type TLSMode int

const (
	TLSAutocert TLSMode = iota
	TLSFiles
)

type Config struct {
	DataDir, Domain, TLSCert, TLSKey, FCMCredentials string
	NativePepper                                     []byte
	GoogleClientIDs                                  []string
	VKServiceToken, OKAppKey, OKAppSecret            string
	FBAppID, FBAppSecret                             string
	BotsEnabled                                      bool
	BotsWait                                         time.Duration
	BotsFile                                         string
	MoveTime, PingInterval, PingTimeout, InviteTTL   time.Duration
	Addr                                             string
}

// Load builds Config from the given environment lookup function.
func Load(getenv func(string) string) (Config, error) {
	c := Config{
		DataDir:         getenv("LPS_DATA_DIR"),
		Domain:          getenv("LPS_DOMAIN"),
		TLSCert:         getenv("LPS_TLS_CERT"),
		TLSKey:          getenv("LPS_TLS_KEY"),
		FCMCredentials:  getenv("LPS_FCM_CREDENTIALS"),
		NativePepper:    []byte(getenv("LPS_NATIVE_PEPPER")),
		VKServiceToken:  getenv("LPS_VK_SERVICE_TOKEN"),
		OKAppKey:        getenv("LPS_OK_APP_KEY"),
		OKAppSecret:     getenv("LPS_OK_APP_SECRET"),
		FBAppID:         getenv("LPS_FB_APP_ID"),
		FBAppSecret:     getenv("LPS_FB_APP_SECRET"),
		BotsFile:        getenv("LPS_BOTS_FILE"),
		Addr:            getenv("LPS_ADDR"),
		GoogleClientIDs: splitList(getenv("LPS_GOOGLE_CLIENT_IDS")),
	}
	if c.DataDir == "" {
		c.DataDir = "."
	}
	if c.Addr == "" {
		c.Addr = ":443"
	}
	if len(c.NativePepper) == 0 {
		return Config{}, errors.New("LPS_NATIVE_PEPPER is required")
	}
	if c.Domain == "" && (c.TLSCert == "" || c.TLSKey == "") {
		return Config{}, errors.New("set LPS_DOMAIN or both LPS_TLS_CERT and LPS_TLS_KEY")
	}
	if v := getenv("LPS_BOTS_ENABLED"); v != "" {
		b, err := strconv.ParseBool(v)
		if err != nil {
			return Config{}, fmt.Errorf("LPS_BOTS_ENABLED: invalid boolean %q", v)
		}
		c.BotsEnabled = b
	}
	for _, d := range []struct {
		name string
		def  int
		dst  *time.Duration
	}{
		{"LPS_MOVE_TIME_SEC", 92, &c.MoveTime},
		{"LPS_PING_INTERVAL_SEC", 5, &c.PingInterval},
		{"LPS_PING_TIMEOUT_SEC", 15, &c.PingTimeout},
		{"LPS_INVITE_TTL_SEC", 60, &c.InviteTTL},
		{"LPS_BOTS_WAIT_SEC", 15, &c.BotsWait},
	} {
		sec := d.def
		if v := getenv(d.name); v != "" {
			n, err := strconv.Atoi(strings.TrimSpace(v))
			if err != nil || n < 0 {
				return Config{}, fmt.Errorf("%s: invalid number of seconds %q", d.name, v)
			}
			sec = n
		}
		*d.dst = time.Duration(sec) * time.Second
	}
	return c, nil
}

// TLS reports how the server obtains its certificate: autocert whenever a
// domain is set, certificate files otherwise.
func (c Config) TLS() TLSMode {
	if c.Domain != "" {
		return TLSAutocert
	}
	return TLSFiles
}

func splitList(s string) []string {
	var out []string
	for _, p := range strings.Split(s, ",") {
		if p = strings.TrimSpace(p); p != "" {
			out = append(out, p)
		}
	}
	return out
}

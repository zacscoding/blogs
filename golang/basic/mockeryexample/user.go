package mockeryexample

import (
	"context"
	"errors"
)

// ErrKeyConflict insert or update 시 키 충돌 시 발생합니다.
var ErrKeyConflict = errors.New("conflict key")

// User 사용자 DB 모델을 나타냅니다.
type User struct {
	Email string
	Name  string
}

//go:generate mockery --name UserDB --case underscore --inpackage
// UserDB 사용자 관련 CRUD 인터페이스를 나타냅니다.
type UserDB interface {
	Save(ctx context.Context, u *User) error
}

type UserService struct {
	userDB UserDB
}

func (us *UserService) Save(ctx context.Context, u *User) error {
	// 간단하게 유효성 검사를 진행합니다.
	if u.Email == "" {
		return errors.New("invalid email")
	}
	if u.Name == "" {
		return errors.New("invalid name")
	}

	// 사용자 데이터를 저장합니다. 중복된 email에 대해서는 다른 에러 메시지를 반환합니다.
	if err := us.userDB.Save(ctx, u); err != nil {
		if errors.Is(err, ErrKeyConflict) {
			return errors.New("duplicate email")
		}
		return err
	}
	return nil
}

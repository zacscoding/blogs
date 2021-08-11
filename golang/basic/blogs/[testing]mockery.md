# Golang Mockery Mock을 이용하여 테스트 하기  

> 전체 소스 코드는 Github[https://github.com/zacscoding/blogs/tree/master/golang/basic/mockeryexample]에 있습니다 :)  

단위 테스트를 진행할 때 의존성이 있는 컴포넌트는 해당 의존성 객체를 Mock으로 사용할 수 있습니다.  

이때 Golang에서 Mock을 생성하는 방법이 (1) 직접 생성 / (2) [stretchr/testify](https://github.com/stretchr/testify) 이용 / (3) [golang/mock](https://github.com/golang/mock) 이용  

하는 방법 정도가 있습니다. 이번 글에서는 (2)번에 있는 testify의 `mock` 패키지와 [mockery(mock 코드 자동 생성)](https://github.com/vektra/mockery)를 이용하여 테스트 하는 방법에 대해 소개하겠습니다.  

## 테스트용 코드  

간단한 기능 테스트를 위해 아래와 같이 사용자 모델(`User`)를 정의하고 저장하는 기능을 구현합니다.  

```go
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
```  

여기서 `UserService` 단위 테스트를 진행할 때 `UserDB` 인터페이스를 Mock 처리할 수 있습니다.  

## Mockery를 이용하여 Mock 코드 생성하기  

`UserDB` 인터페이스에 아래와 같이 `go:generate mockery` 주석을 추가합니다.  

```go
//go:generate mockery --name UserDB --case underscore --inpackage
// UserDB 사용자 관련 CRUD 인터페이스를 나타냅니다.
type UserDB interface {
	...
}
```  

여기서는 `name(generate 매칭 이름)`, `case(파일 이름)`, `inpackage(동일 패키지)` flag만 사용했습니다.  

자세한 설명은 [mockery](https://github.com/vektra/mockery#extended-flag-descriptions)에서 확인할 수 있습니다.  

다음으로 `go generate ./...` 를 실행하면 아래와 같이 `mock_user_db.go`가 생성되는 것을 확인할 수 있습니다.  

```shell
$ go generate ./...
12 Aug 21 00:10 KST INF Starting mockery dry-run=false version=(devel)
12 Aug 21 00:10 KST INF Walking dry-run=false version=(devel)
12 Aug 21 00:10 KST INF Generating mock dry-run=false interface=UserDB qualified-name=github.com/zacscoding/blogs/basic/mockeryexample version=(devel)

$ tree ./mockeryexample
./mockeryexample
├── mock_user_db.go     <-- 생성된 파일
└── user.go
```  

생성 된 코드를 확인해보면 해당 인터페이스 함수를 모두 구현한 것을 확인할 수 있습니다.  

```go
// Code generated by mockery (devel). DO NOT EDIT.

package mockeryexample

import (
	context "context"

	mock "github.com/stretchr/testify/mock"
)

// MockUserDB is an autogenerated mock type for the UserDB type
type MockUserDB struct {
	mock.Mock
}

// Save provides a mock function with given fields: ctx, u
func (_m *MockUserDB) Save(ctx context.Context, u *User) error {
	ret := _m.Called(ctx, u)

	var r0 error
	if rf, ok := ret.Get(0).(func(context.Context, *User) error); ok {
		r0 = rf(ctx, u)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}
```  

## 단위 테스트 코드 작성하기(성공)  

우선 모든 테스트에서 `MockUserDB`와 해당 Mock을 이용하는 `UserService` 필요하기때문에 아래와 같이 `fixtures()` 함수를 정의합니다.  

```go
func fixtures() (s *UserService, m *MockUserDB) {
	m = &MockUserDB{}
	s = &UserService{
		userDB: m,
	}
	return s, m
}
```  

다음으로는 `Save` 성공에 대한 테스트 코드와 설명입니다.  

```go
func TestSave(t *testing.T) {
	// given
	// 위에서 정의한 `fixtures()` 함수를 이용하여 `MockUserDB`, `UserService` 인스턴스를 Setup합니다.
	s, m := fixtures()
	// 테스트에서 사용할 User 모델을 정의합니다.
	user1 := User{
		Email: "user1@gmail.com",
		Name:  "user1",
	}
	userMatcher := func(u *User) bool {
		return u.Email == user1.Email && u.Name == user1.Name
	}
	m.On("Save", mock.Anything, mock.MatchedBy(userMatcher)).Return(nil)

	// when
	err := s.Save(context.TODO(), &user1)

	// then
	assert.NoError(t, err)
	m.AssertCalled(t, "Save", mock.Anything, mock.MatchedBy(userMatcher))
}
```  

### Given

위의 코드에서 `m.On("Save", mock.Anything, mock.MatchedBy(userMatcher)).Return(nil)`를 살펴보면  

`MockUserDB`의 Save 함수 호출에 대하여 예상하는 Argument와 Return을 정의합니다.  

첫번째 인자인 context.Context는 `mock.Anything`을 이용하여 매칭 여부를 체크하지 않고, `mock.MatchedBy(userMatcher)`를 통해  

user1의 email, name이 매칭되는 경우에만 nil(`.Return(nil)`)을 리턴합니다.  

만약 여기서 정의하지 않은 user2로 Save()를 시도하면 아래와 같은 패닉과 에러가 발생합니다.  

```text
mock: Unexpected Method Call
-----------------------------

Save(*context.emptyCtx,*mockeryexample.User)
		0: (*context.emptyCtx)(0xc0000a2010)
		1: &mockeryexample.User{Email:"user2", Name:"user2"}

The closest call I have is: 

Save(string,mock.argumentMatcher)
		0: "mock.Anything"
		1: mock.argumentMatcher{fn:reflect.Value{typ:(*reflect.rtype)(0x11c72a0), ptr:(unsafe.Pointer)(0xc000099030), flag:0x13}}
```  

### Then  

다음으로 테스트 결과에 대해 검증합니다. 우선 Save 함수를 호출하면 에러가 없으므로 `assert.NoError(t, err)`로 검증합니다.  

또한 실제 `UserDB` 인터페이스가 원하는 Argument로 호출되었는지 검증하기 위해 `m.AssertCalled(t, "Save", mock.Anything, mock.MatchedBy(userMatcher))`를 정의합니다.  

예제를 위해 `m.AssertNumberOfCalls(t, "Save", 1)` Mock의 Save 함수 전체 호출 수도 검증합니다.  

## 실패 단위 테스트 코드 작성하기  

golang에서는 하나의 함수 내에서 구조체 정의와 `testing.T`의 `t.Run()` 함수를 통해 아래와 같이 다양한 케이스를 실행할 수 있습니다.  

```go
func TestSave_Fail(t *testing.T) {
	// 실패 테스트 케이스에 대한 구조체 정의
	cases := []struct {
		Name       string
		User       User
		SetupMock  func(m *MockUserDB)
		Msg        string
        AssertMock func(t *testing.T, m *MockUserDB)
	}{
	    // 여기에 실패 테스트 정의 및 검증	
    }

	for _, tc := range cases {
		t.Run(tc.Name, func(t *testing.T) {
			// t.Parallel() // 위의 TestCases를 병렬로 실행할 수 있습니다.
			// given
			s, m := fixtures()
			if tc.SetupMock != nil {
				tc.SetupMock(m)
			}

			// when
			err := s.Save(context.TODO(), &tc.User)

			// then
			assert.Error(t, err)
			if tc.AssertMock != nil {
                tc.AssertMock(t, m)
			}
		})
	}
}
```  

테스트 케이스를 살펴보기 위해 `UserService` 코드를 다시 살펴보겠습니다.  

```text
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
```  

여기서 우리가 테스트를 실패하는 케이스(즉 error를 리턴하는)의 error 반환은 아래와 같이 다를 수 있습니다.  

- User 모델의 Email이 없는 경우 -> `invalid email` 메시지의 에러 반환
- User 모델의 Name이 없는 경우 -> `invalid name` 메시지의 에러 반환
- UserDB에서 `ErrKeyConflict`를 반환하는 경우 -> `duplicate email` 메시지의 에러 반환
- UserDB에서 다른 에러를 반환하는 경우 -> err 그대로 반환  

위의 내용을 TestCases에 작성하면 아래와 같습니다.  

```go
func TestSave_Fail(t *testing.T) {
	cases := []struct {
		Name       string
		User       User
		SetupMock  func(m *MockUserDB)
		Msg        string
		AssertMock func(t *testing.T, m *MockUserDB)
	}{
		{
			Name: "empty email",
			User: User{},
			Msg:  "invalid email",
			AssertMock: func(t *testing.T, m *MockUserDB) {
				m.AssertNotCalled(t, "Save")
			},
		}, {
			Name: "empty name",
			User: User{Email: "user1@email.com"},
			Msg:  "invalid name",
			AssertMock: func(t *testing.T, m *MockUserDB) {
				m.AssertNotCalled(t, "Save")
			},
		}, {
			Name: "duplicate email",
			User: User{Email: "user1@email.com", Name: "user1"},
			SetupMock: func(m *MockUserDB) {
				m.On("Save", mock.Anything, mock.Anything).Return(ErrKeyConflict)
			},
			Msg: "duplicate email",
			AssertMock: func(t *testing.T, m *MockUserDB) {
				m.AssertNumberOfCalls(t, "Save", 1)
			},
		}, {
			Name: "any error",
			User: User{Email: "user1@email.com", Name: "user1"},
			SetupMock: func(m *MockUserDB) {
				m.On("Save", mock.Anything, mock.Anything).Return(errors.New("any error"))
			},
			Msg: "any error",
			AssertMock: func(t *testing.T, m *MockUserDB) {
				m.AssertNumberOfCalls(t, "Save", 1)
			},
		},
	}
	...
}	
```

아래의 사진은 Goland에서 실행한 결과입니다.  

![Test Result](https://user-images.githubusercontent.com/25560203/129062804-68b7d847-af83-4143-8ff8-1ea0c06bf49e.png)  

위와 같이 테스트 케이스를 작성하다보면 하나씩 돌리고 싶을때가 있는데요,  

이때는 아래와 같이 `{ }`안에 마우스를 클릭하여 실행하면 하나의 TC만 실행할 수 있습니다 :)  

![Run One TC](https://user-images.githubusercontent.com/25560203/129063345-8122c384-fd70-465d-8290-100dc2184870.png)  

(`go test -v -run TestSave_Fail/any_error` 명령어와 같습니다.)  

---  

이번 포스팅에서는 Golang의 Mock 처리 방법에 대해 알아봤습니다.  

Mock과 Mockery는 제가 단위 테스트에서 주로 사용하고 있는데 필요한 기능은 대부분은 지원하고 있어서 테스트에 무리는 없었던 것 같습니다.  

하지만 아쉬운점은 코드 생성이 필요하고 가끔 mockito의 Answer와 같은 기능이 필요할 때가 있는데, 이부분은 아직 해결법을 찾지는 못한 것 같습니다 :(  

다음 포스팅에서는 [golang/mock](https://github.com/golang/mock)에 대한 내용으로 포스팅하겠습니다. 감사합니다.